package xyz.medirec.medirec.pojo

import java.io.Serializable
import java.security.KeyPair

class MediRec(val timeSet : Set<String>, val keyPair: KeyPair, val randomString: String, val salt: String): Serializable {
    companion object { const val serialVersionUID = 415415415415L }
}