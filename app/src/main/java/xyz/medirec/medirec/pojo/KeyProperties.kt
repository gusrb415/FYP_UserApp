package xyz.medirec.medirec.pojo

import java.io.Serializable

class KeyProperties(val encoded: ByteArray): Serializable {
    companion object {
        const val serialVersionUID = 1234123412341L
    }
}