package cn.entertech.affectiveclouddemo.model.meditation;

class RealtimeEEGDataEntity {
    var leftwave: ArrayList<Double>? = null
    var rightwave: ArrayList<Double>? = null
    var alphaPower: Double? = null
    var betaPower: Double? = null
    var thetaPower: Double? = null
    var deltaPower: Double? = null
    var gammaPower: Double? = null
    var progress: Double? = null
    override fun toString(): String {
        return "RealtimeEEGDataEntity(leftwave=$leftwave, rightwave=$rightwave, alphaPower=$alphaPower, betaPower=$betaPower, thetaPower=$thetaPower, deltaPower=$deltaPower, gammaPower=$gammaPower, progress=$progress)"
    }


}