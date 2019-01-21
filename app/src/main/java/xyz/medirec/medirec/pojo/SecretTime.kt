package xyz.medirec.medirec.pojo

import java.io.Serializable

class SecretTime(val timestamp: Long, val secretKeyEncoded: ByteArray) : Serializable {
    companion object {
        const val serialVersionUID = -41233124123123L
    }
}