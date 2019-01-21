package xyz.medirec.medirec.pojo

import java.io.Serializable

class SignatureKey (val signature: ByteArray, val keyEncoded: ByteArray, val timestamp: Long) :Serializable{
    companion object {
        const val serialVersionUID = 11235343124123123L
    }
}