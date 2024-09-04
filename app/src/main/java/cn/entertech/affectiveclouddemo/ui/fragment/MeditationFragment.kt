package cn.entertech.affectiveclouddemo.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import cn.entertech.affectiveclouddemo.R
import cn.entertech.affectiveclouddemo.app.Constant.Companion.MEDITATION_TYPE
import cn.entertech.affectiveclouddemo.model.MessageEvent
import cn.entertech.affectiveclouddemo.ui.activity.MeditationActivity
import cn.entertech.affectiveclouddemo.ui.activity.SensorContactCheckActivity
import cn.entertech.affectiveclouddemo.ui.view.MeditationBrainwaveView
import cn.entertech.affectiveclouddemo.ui.view.MeditationEmotionView
import cn.entertech.affectiveclouddemo.ui.view.MeditationHeartView
import cn.entertech.affectiveclouddemo.ui.view.MeditationInterruptView
import cn.entertech.affectiveclouddemo.utils.DataSender
import cn.entertech.affectiveclouddemo.utils.DataSender2
import cn.entertech.affectiveclouddemo.utils.MeditationStatusPlayer
import cn.entertech.affectivecloudsdk.entity.RealtimeEEGData
import cn.entertech.bleuisdk.ui.DeviceUIConfig
import cn.entertech.bleuisdk.ui.activity.DeviceManagerActivity
import cn.entertech.uicomponentsdk.utils.ScreenUtil
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.util.Collections


class MeditationFragment : MeditationBaseFragment() {
    private lateinit var dataSender: DataSender
    private var isToConnect: Boolean = false
    private var isMeditationInterrupt: Boolean = false
    var selfView: View? = null
    var llContainer: LinearLayout? = null
    var isHeartViewLoading = true
    var isBreathCoherenceLoading = true
    var isBrainViewLoading = true
    var isAttentionLoading = true
    var isRelaxationLoading = true
    var isPressureLoading = true
    var isArousalLoading = true
    var isPleasureLoading = true
    var isCoherenceLoading = true

    var isTimerScheduling = false
    var isBleConnected: Boolean = false
    var meditationStatusPlayer: MeditationStatusPlayer? = null
    companion object {
        val SHOW_LOADING_TIME_DELAY = 3000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化 DataSender
        dataSender = DataSender("192.168.235.33", 65432) // 用你的服务器 IP 和端口.替换
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        selfView = inflater.inflate(R.layout.fragment_data, container, false)
        meditationStatusPlayer = MeditationStatusPlayer(activity!!)
        initView()
        return selfView
    }

    fun initView() {
        selfView?.findViewById<ImageView>(R.id.iv_close)?.setOnClickListener {
            (activity!! as MeditationActivity).showDialog()
        }
        selfView?.findViewById<TextView>(R.id.tv_edit)?.setOnClickListener {
            var messageEvent = MessageEvent()
            messageEvent.messageCode = MessageEvent.MESSAGE_CODE_DATA_EDIT
            messageEvent.message = "edit"
            EventBus.getDefault().post(messageEvent)
        }

        refreshMeditationView()

        if ((activity as MeditationActivity).bleManager.isConnected()) {
            handleDeviceConnect()
        } else {
            handleDeviceDisconnect()
        }

        selfView?.findViewById<MeditationInterruptView>(R.id.miv_interrupt_device)
            ?.addErrorMessageListener {
                isMeditationInterrupt = true
                resetLoading()
                selfView?.findViewWithTag<MeditationBrainwaveView>("Brainwave")
                    ?.showErrorMessage(it)
                selfView?.findViewWithTag<MeditationHeartView>("Heart")?.showErrorMessage(it)
                selfView?.findViewWithTag<MeditationEmotionView>("Emotion")?.showErrorMessage(it)
            }
        DeviceUIConfig.getInstance(activity!!).managers[0].addContactListener(::onBleContactListener)
    }

    var showBrainLoadingRunnable = Runnable {
        selfView?.findViewWithTag<MeditationBrainwaveView>("Brainwave")
            ?.showLoadingCover()
    }

    var showHRLoadingRunnable = Runnable {
        selfView?.findViewWithTag<MeditationHeartView>("Heart")?.showHRLoadingCover()
    }

    var showBreathCohLoadingRunnable = Runnable {
        selfView?.findViewWithTag<MeditationHeartView>("Heart")?.showBreathCoherenceLoadingCover()
    }

    var showAttentionLoadingRunnable = Runnable {
        selfView?.findViewWithTag<MeditationEmotionView>("Emotion")
            ?.showAttentionLoading()
    }

    var showRelaxationLoadingRunnable = Runnable {
        selfView?.findViewWithTag<MeditationEmotionView>("Emotion")
            ?.showRelaxationLoading()
    }

    var showPressureLoadingRunnable = Runnable {
        selfView?.findViewWithTag<MeditationEmotionView>("Emotion")
            ?.showPressureLoading()
    }
    var showArousalLoadingRunnable = Runnable {
        selfView?.findViewWithTag<MeditationEmotionView>("Emotion")
            ?.showArousalLoading()
    }
    var showCoherenceLoadingRunnable = Runnable {
        selfView?.findViewWithTag<MeditationEmotionView>("Emotion")
            ?.showCoherenceLoading()
    }
    var showPleasureLoadingRunnable = Runnable {
        selfView?.findViewWithTag<MeditationEmotionView>("Emotion")
            ?.showPleasureLoading()
    }

    fun refreshMeditationView() {
        llContainer = selfView?.findViewById<LinearLayout>(R.id.ll_container)
        llContainer?.removeAllViews()
        var viewOrders = "Heart,Brainwave,Emotion".split(",")
        var lp = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        lp.leftMargin = ScreenUtil.dip2px(activity!!, 16f)
        lp.rightMargin = ScreenUtil.dip2px(activity!!, 16f)
        lp.topMargin = ScreenUtil.dip2px(activity!!, 16f)
        for (i in 0 until viewOrders.size) {
            when (viewOrders[i]) {
                "Emotion" -> {
                    var meditationEmotionView = MeditationEmotionView(activity!!)
                    meditationEmotionView.tag = viewOrders[i]
                    meditationEmotionView.layoutParams = lp
                    llContainer?.addView(meditationEmotionView)
                }
                "Heart" -> {
                    var meditationHeartView = MeditationHeartView(activity!!)
                    meditationHeartView.tag = viewOrders[i]
                    meditationHeartView.layoutParams = lp
                    llContainer?.addView(meditationHeartView)
                }
                "Brainwave" -> {
                    var meditationBrainwaveView = MeditationBrainwaveView(activity!!)
                    meditationBrainwaveView.tag = viewOrders[i]
                    meditationBrainwaveView.layoutParams = lp
                    llContainer?.addView(meditationBrainwaveView)
                }
            }
        }
    }

    override fun showBrain(realtimeEEGDataEntity: RealtimeEEGData?) {
        activity?.runOnUiThread {
            selfView?.findViewWithTag<MeditationBrainwaveView>("Brainwave")
                ?.setLeftBrainwave(realtimeEEGDataEntity?.leftwave)
            selfView?.findViewWithTag<MeditationBrainwaveView>("Brainwave")
                ?.setRightBrainwave(realtimeEEGDataEntity?.rightwave)
            selfView?.findViewWithTag<MeditationBrainwaveView>("Brainwave")
                ?.setAlphaWavePercent(realtimeEEGDataEntity?.alphaPower?.toFloat())
            selfView?.findViewWithTag<MeditationBrainwaveView>("Brainwave")
                ?.setBetaWavePercent(realtimeEEGDataEntity?.betaPower?.toFloat())
            selfView?.findViewWithTag<MeditationBrainwaveView>("Brainwave")
                ?.setDeltaWavePercent(realtimeEEGDataEntity?.deltaPower?.toFloat())
            selfView?.findViewWithTag<MeditationBrainwaveView>("Brainwave")
                ?.setGammaWavePercent(realtimeEEGDataEntity?.gammaPower?.toFloat())
            selfView?.findViewWithTag<MeditationBrainwaveView>("Brainwave")
                ?.setThetaWavePercent(realtimeEEGDataEntity?.thetaPower?.toFloat())
            if (realtimeEEGDataEntity?.leftwave == null || realtimeEEGDataEntity.leftwave!!.size == 0
                || realtimeEEGDataEntity.rightwave == null || realtimeEEGDataEntity.rightwave!!.size == 0
            ) {
                return@runOnUiThread
            }


            isBrainViewLoading =
                Collections.max(realtimeEEGDataEntity.leftwave) == 0.0 && Collections.max(
                    realtimeEEGDataEntity.rightwave
                ) == 0.0
            if (!isMeditationInterrupt) {
                if (isBrainViewLoading) {
                    mMainHandler.postDelayed(showBrainLoadingRunnable, SHOW_LOADING_TIME_DELAY)
                } else {
                    mMainHandler.removeCallbacks(showBrainLoadingRunnable)
                    selfView?.findViewWithTag<MeditationBrainwaveView>("Brainwave")
                        ?.hindLoadingCover()
                }
            }
        }
    }

    override fun showHeart(heartRate: Int?) {
        if (heartRate == null) {
            return
        }
        activity?.runOnUiThread {
            isHeartViewLoading = heartRate == 0
            if (heartRate != 0) {
                selfView?.findViewWithTag<MeditationHeartView>("Heart")?.setHeartValue(heartRate)
                val jsonBuilder = GsonBuilder().create()
                val hrData = mapOf("value" to heartRate)
                val hrDataJson = jsonBuilder.toJson(hrData)

                // 使用协程来发送数据
                CoroutineScope(Dispatchers.IO).launch {
                    hrDataJson?.let { json ->
                        dataSender.send("HRData: $json\n")
                    }
                }
            }
            if (!isMeditationInterrupt) {
                if (isHeartViewLoading) {
                    mMainHandler.postDelayed(showHRLoadingRunnable, SHOW_LOADING_TIME_DELAY)
                } else {
                    mMainHandler.removeCallbacks(showHRLoadingRunnable)
                    selfView?.findViewWithTag<MeditationHeartView>("Heart")?.hideHRLoadingCover()
                }
            }
        }
    }


    override fun showBreathCoherence(hr: Double?){
        if (hr == null){
            return
        }
        isBreathCoherenceLoading = hr == 0.0
        if (hr != 0.0){
            selfView?.findViewWithTag<MeditationHeartView>("Heart")?.setBreathCoherence(listOf(hr))

        }
        if (!isMeditationInterrupt) {
            if (isBreathCoherenceLoading) {
                mMainHandler.postDelayed(showBreathCohLoadingRunnable, SHOW_LOADING_TIME_DELAY)
            } else {
                mMainHandler.removeCallbacks(showBreathCohLoadingRunnable)
                selfView?.findViewWithTag<MeditationHeartView>("Heart")?.hideBreathCoherenceLoadingCover()
            }

        }
    }

    override fun showAttention(attention: Float?) {
        if (attention == null) {
            return
        }
        activity?.runOnUiThread {
            selfView?.findViewWithTag<MeditationEmotionView>("Emotion")?.setAttention(attention)
            val jsonBuilder = GsonBuilder().create()
            val attentionData = mapOf("value" to attention)
            val attentionDataJson = jsonBuilder.toJson(attentionData)

            // 使用协程来发送数据
            CoroutineScope(Dispatchers.IO).launch {
                attentionDataJson?.let { json ->
                    dataSender.send("AttentionData: $json\n")
                }
            }
            isAttentionLoading = attention == 0f
            if (!isMeditationInterrupt) {
                if (isAttentionLoading) {
                    mMainHandler.postDelayed(showAttentionLoadingRunnable, SHOW_LOADING_TIME_DELAY)
                } else {
                    mMainHandler.removeCallbacks(showAttentionLoadingRunnable)
                    selfView?.findViewWithTag<MeditationEmotionView>("Emotion")
                        ?.hideAttentionLoaidng()
                }
            }
        }
    }

    override fun showRelaxation(relaxation: Float?) {
        if (relaxation == null) {
            return
        }
        activity?.runOnUiThread {
            selfView?.findViewWithTag<MeditationEmotionView>("Emotion")?.setRelaxation(relaxation)
            val jsonBuilder = GsonBuilder().create()
            val relaxationData = mapOf("value" to relaxation)
            val relaxationDataJson = jsonBuilder.toJson(relaxationData)

            // 使用协程来发送数据
            CoroutineScope(Dispatchers.IO).launch {
                relaxationDataJson?.let { json ->
                    dataSender.send("RelaxationData: $json\n")
                }
            }
            isRelaxationLoading = relaxation == 0f

            if (!isMeditationInterrupt) {
                if (isRelaxationLoading) {
                    mMainHandler.postDelayed(showRelaxationLoadingRunnable, SHOW_LOADING_TIME_DELAY)
                } else {
                    mMainHandler.removeCallbacks(showRelaxationLoadingRunnable)
                    selfView?.findViewWithTag<MeditationEmotionView>("Emotion")
                        ?.hideRelaxationLoaidng()
                }
            }

        }
    }

    override fun showPressure(pressure: Float?) {
        if (pressure == null) {
            return
        }
        activity?.runOnUiThread {
            selfView?.findViewWithTag<MeditationEmotionView>("Emotion")
                ?.setStress(pressure+70)
            val jsonBuilder = GsonBuilder().create()
            val pressureData = mapOf("value" to pressure)
            val pressureDataJson = jsonBuilder.toJson(pressureData)

            // 使用协程来发送数据
            CoroutineScope(Dispatchers.IO).launch {
                pressureDataJson?.let { json ->
                    dataSender.send("PressureData: $json\n")
                }
            }
            isPressureLoading = pressure == 0f
            if (!isMeditationInterrupt) {
                if (isPressureLoading) {
                    mMainHandler.postDelayed(showPressureLoadingRunnable, SHOW_LOADING_TIME_DELAY)
                } else {
                    mMainHandler.removeCallbacks(showPressureLoadingRunnable)
                    selfView?.findViewWithTag<MeditationEmotionView>("Emotion")
                        ?.hidePressureLoaidng()
                }
            }
        }
    }

    override fun showArousal(arousal: Float?) {
        if (arousal == null) {
            return
        }
        activity?.runOnUiThread {
            selfView?.findViewWithTag<MeditationEmotionView>("Emotion")
                ?.setArousal(arousal)
            val jsonBuilder = GsonBuilder().create()
            val arousalData = mapOf("value" to arousal)
            val arousalDataJson = jsonBuilder.toJson(arousalData)

            // 使用协程来发送数据
            CoroutineScope(Dispatchers.IO).launch {
                arousalDataJson?.let { json ->
                    dataSender.send("ArousalData: $json\n")
                }
            }
            isArousalLoading = arousal == 0f
            if (!isMeditationInterrupt) {
                if (isArousalLoading) {
                    mMainHandler.postDelayed(showArousalLoadingRunnable, SHOW_LOADING_TIME_DELAY)
                } else {
                    mMainHandler.removeCallbacks(showArousalLoadingRunnable)
                    selfView?.findViewWithTag<MeditationEmotionView>("Emotion")
                        ?.hideArousalLoaidng()
                }
            }
        }
    }

    override fun showPleasure(pleasure: Float?) {
        if (pleasure == null) {
            return
        }
        activity?.runOnUiThread {
            selfView?.findViewWithTag<MeditationEmotionView>("Emotion")
                ?.setPleasure(pleasure)
            val jsonBuilder = GsonBuilder().create()
            val pleasureData = mapOf("value" to pleasure)
            val pleasureDataJson = jsonBuilder.toJson(pleasureData)

            // 使用协程来发送数据
            CoroutineScope(Dispatchers.IO).launch {
                pleasureDataJson?.let { json ->
                    dataSender.send("PleasureData: $json\n")
                }
            }
            isPleasureLoading = pleasure == 0f
            if (!isMeditationInterrupt) {
                if (isPleasureLoading) {
                    mMainHandler.postDelayed(showPleasureLoadingRunnable, SHOW_LOADING_TIME_DELAY)
                } else {
                    mMainHandler.removeCallbacks(showPleasureLoadingRunnable)
                    selfView?.findViewWithTag<MeditationEmotionView>("Emotion")
                        ?.hidePleasureLoaidng()
                }
            }
        }
    }

    override fun showCoherence(coherence: Float?) {
        if (coherence == null) {
            return
        }
        activity?.runOnUiThread {
            selfView?.findViewWithTag<MeditationEmotionView>("Emotion")
                ?.setCoherence(coherence)
            val jsonBuilder = GsonBuilder().create()
            val coherenceData = mapOf("value" to coherence)
            val coherenceDataJson = jsonBuilder.toJson(coherenceData)

            // 使用协程来发送数据
            CoroutineScope(Dispatchers.IO).launch {
                coherenceDataJson?.let { json ->
                    dataSender.send("CoherenceData: $json\n")
                }
            }
            isCoherenceLoading = coherence == 0f
            if (!isMeditationInterrupt) {
                if (isCoherenceLoading) {
                    mMainHandler.postDelayed(showCoherenceLoadingRunnable, SHOW_LOADING_TIME_DELAY)
                } else {
                    mMainHandler.removeCallbacks(showCoherenceLoadingRunnable)
                    selfView?.findViewWithTag<MeditationEmotionView>("Emotion")
                        ?.hideCoherenceLoaidng()
                }
            }
        }
    }


    var goodContactCount = 0
    var isGoodContact = false
    private fun onBleContactListener(contact: Int) {
//        Log.d("#######", "contact is $contact")
        activity!!.runOnUiThread {
            if (contact != 0) {
                isGoodContact = false
                goodContactCount = 0
                isTimerScheduling = true
                mMainHandler.postDelayed(runnable, 1000)
            } else {
                isTimerScheduling = false
                mMainHandler.removeCallbacks(runnable)
                goodContactCount++
                if (goodContactCount == 5) {
                    if (isMeditationInterrupt) {
                        hideInterruptTip()
                        isGoodContact = true
                        goodContactCount = 0
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isToConnect) {
            mMainHandler.postDelayed(Runnable {
                toSensorCheckActivity()
                isToConnect = false
            }, 1000)
        }
    }
    var runnable = Runnable {
        handleInterruptTip()
    }
    var mMainHandler = Handler(Looper.getMainLooper())
    private var lastQuality: Double = 0.0
    override fun dealQuality(quality: Double?) {
        if (quality == null) {
            return
        }
        if (quality >= 2.0) {
            isTimerScheduling = false
            mMainHandler.removeCallbacks(runnable)
//            if (lastQuality < 2 && isMeditationInterrupt) {
//                handleInterruptTip()
//            }
        } else {
            if (isGoodContact && !isTimerScheduling) {
                isTimerScheduling = true
                mMainHandler.postDelayed(runnable, 30000)
            }
        }
        lastQuality = quality
    }

    fun dataReset() {
        showRelaxation(0f)
        showAttention(0f)
        showPressure(0f)
        showArousal(0f)
        showHeart(0)
        selfView?.findViewWithTag<MeditationBrainwaveView>("Brainwave")?.showLoadingCover()
    }

    var toConnectDeviceListener = fun() {
        isToConnect = true
        startActivity(Intent(activity, DeviceManagerActivity::class.java))
    }


    fun showSampleData() {
        selfView?.findViewWithTag<MeditationEmotionView>("Emotion")?.showSampleData()
        selfView?.findViewWithTag<MeditationHeartView>("Heart")?.showHRSampleData()
        selfView?.findViewWithTag<MeditationBrainwaveView>("Brainwave")?.showSampleData()
    }

    fun hideSampleData() {
        selfView?.findViewWithTag<MeditationEmotionView>("Emotion")?.hideSampleData()
        selfView?.findViewWithTag<MeditationHeartView>("Heart")?.hideHRSampleData()
        selfView?.findViewWithTag<MeditationBrainwaveView>("Brainwave")?.hideSampleData()
        showLoadingCover()
    }

    fun showLoadingCover() {
        selfView?.findViewWithTag<MeditationEmotionView>("Emotion")?.showAttentionLoading()
        selfView?.findViewWithTag<MeditationEmotionView>("Emotion")?.showRelaxationLoading()
        selfView?.findViewWithTag<MeditationEmotionView>("Emotion")?.showPressureLoading()
        selfView?.findViewWithTag<MeditationEmotionView>("Emotion")?.showArousalLoading()
        selfView?.findViewWithTag<MeditationHeartView>("Heart")?.showHRLoadingCover()
        selfView?.findViewWithTag<MeditationBrainwaveView>("Brainwave")?.showLoadingCover()
    }

    fun resetLoading() {
        isHeartViewLoading = true
        isBrainViewLoading = true
        isAttentionLoading = true
        isRelaxationLoading = true
        isPressureLoading = true
    }

    fun toSensorCheckActivity(){
        if (activity is MeditationActivity) {
            activity!!.startActivity(
                Intent(activity!!, SensorContactCheckActivity::class.java).putExtra(
                    MEDITATION_TYPE,
                    "meditation"
                )
            )
        } else {
            activity!!.startActivity(
                Intent(activity!!, SensorContactCheckActivity::class.java).putExtra(
                    MEDITATION_TYPE,
                    "meditation"
                )
            )
        }
    }

    var toSignalCheckListener = fun() {
        toSensorCheckActivity()
    }

    var toNetRestoreLinstener = fun() {
        (activity as MeditationActivity).restore()
    }

    var isFirstIn = true
    fun playConnectAudio() {
        if (!isFirstIn && selfView?.findViewById<MeditationInterruptView>(R.id.miv_interrupt_device)
                ?.visibility ==
            View.VISIBLE
        ) {
            meditationStatusPlayer?.playConnectAudio()
        }
    }

    fun playDisconnectAudio() {
        if (!isFirstIn && selfView?.findViewById<MeditationInterruptView>(R.id.miv_interrupt_device)
                ?.visibility ==
            View.GONE
        ) {
            meditationStatusPlayer?.playDisconnectAudio()
        }
    }

    fun hideInterruptTip() {
        isTimerScheduling = false
        playConnectAudio()
        isMeditationInterrupt = false
        showLoadingCover()
        selfView?.findViewById<MeditationInterruptView>(R.id.miv_interrupt_device)
            ?.visibility =
            View.GONE
    }

    override fun handleInterruptTip() {
        if (activity == null){
            return
        }
        isBleConnected = DeviceUIConfig.getInstance(activity!!).managers[0].isConnected()
        if (isBleConnected) {
            if ((activity as MeditationActivity).enterAffectiveCloudManager!!.isWebSocketOpen()) {
                if (isTimerScheduling) {
                    playDisconnectAudio()
                    selfView?.findViewById<MeditationInterruptView>(R.id.miv_interrupt_device)
                        ?.visibility =
                        View.VISIBLE
                    selfView?.findViewById<MeditationInterruptView>(R.id.miv_interrupt_device)
                        ?.toSignalBad(toSignalCheckListener)
                } else {
                    hideInterruptTip()
                }
            } else {
                playDisconnectAudio()
                selfView?.findViewById<MeditationInterruptView>(R.id.miv_interrupt_device)
                    ?.visibility =
                    View.VISIBLE
                selfView?.findViewById<MeditationInterruptView>(R.id.miv_interrupt_device)
                    ?.toNetDisconnect(toNetRestoreLinstener)
            }
        } else {
            playDisconnectAudio()
            selfView?.findViewById<MeditationInterruptView>(R.id.miv_interrupt_device)?.visibility =
                View.VISIBLE
            selfView?.findViewById<MeditationInterruptView>(R.id.miv_interrupt_device)
                ?.toDeviceDisconnect(toConnectDeviceListener)
        }
    }

    override fun handleDeviceDisconnect() {
        handleInterruptTip()
    }

    override fun handleDeviceConnect() {
        handleInterruptTip()
    }

    override fun handleWebSocketDisconnect() {
        handleInterruptTip()
    }

    override fun handleWebSocketConnect() {
        handleInterruptTip()
    }

    override fun onDestroy() {
        DeviceUIConfig.getInstance(activity!!).managers[0].removeContactListener(::onBleContactListener)
        super.onDestroy()
    }

}
