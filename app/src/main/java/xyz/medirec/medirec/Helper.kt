package xyz.medirec.medirec

import android.graphics.Bitmap
import android.util.Base64
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.ImageView
import com.google.zxing.BarcodeFormat
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.util.encoders.Hex
import java.io.*
import java.security.MessageDigest
import java.security.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import java.security.PrivateKey
import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.crypto.params.ECDomainParameters
import org.spongycastle.crypto.params.ECPrivateKeyParameters
import org.spongycastle.crypto.signers.ECDSASigner
import org.spongycastle.crypto.signers.HMacDSAKCalculator
import org.spongycastle.jce.ECNamedCurveTable
import java.security.interfaces.ECPrivateKey
import java.security.spec.ECGenParameterSpec
import javax.crypto.spec.IvParameterSpec


object Helper {

    init {
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }

    fun serialize(any: Any): ByteArray {
        val baos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(baos)
        oos.writeObject(any)

        return baos.toByteArray()
    }

    fun deserialize(serializedString: String): Any {
        return deserialize(decodeFromString(serializedString))
    }

    fun deserialize(byteArray: ByteArray): Any {
        val bais = ByteArrayInputStream(byteArray)
        val ois = ObjectInputStream(bais)

        return ois.readObject()
    }

    fun encodeToString(byteArray: ByteArray): String {
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    fun decodeFromString(base64String: String): ByteArray {
        return Base64.decode(base64String, Base64.NO_WRAP)
    }

    fun generateKeyPair(algorithm: String): KeyPair {
        val keyGen = KeyPairGenerator.getInstance(algorithm)
        val keySize = if(algorithm == "ECDSA" || algorithm == "EC") 256 else 2048
        if(algorithm == "EC")
            keyGen.initialize(ECGenParameterSpec("secp256k1"))
        else
            keyGen.initialize(keySize)
        return keyGen.generateKeyPair()
    }

    fun drawQRCode(view: ImageView, string: String, windowManager: WindowManager) {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = (displayMetrics.heightPixels * 0.5).toInt()
        val width = kotlin.math.min(height, displayMetrics.widthPixels)

        val bitMatrix = QRCodeWriter().encode(string, BarcodeFormat.QR_CODE, height, width)
        val bitMap = encodeAsBitmap(bitMatrix) ?: return
        view.setImageBitmap(bitMap)
    }

    fun drawQRCode(view: ImageView, byteArray: ByteArray, windowManager: WindowManager) {
        drawQRCode(view, Helper.encodeToString(byteArray), windowManager)
    }

    fun getAESKey(key: PrivateKey, index: String, randomString: String) : SecretKey {
        return generateSecretKey((getHash(key.encoded) + index).toCharArray(), (index + randomString).toByteArray())
    }

    fun encrypt(content: ByteArray, secretKey: SecretKey): ByteArray {
        val keySpec = SecretKeySpec(secretKey.encoded, "AES")
        val ivParameterSpec = IvParameterSpec(ByteArray(16))
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec)
        return cipher.doFinal(content)
    }

    fun decrypt(encrypted: ByteArray, secretKey: SecretKey): ByteArray {
        val keySpec = SecretKeySpec(secretKey.encoded, "AES")
        val ivParameterSpec = IvParameterSpec(ByteArray(16))
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec)
        return cipher.doFinal(encrypted)
    }

    private fun encodeAsBitmap(result: BitMatrix): Bitmap? {
        val white = 0xFFFFFFFF.toInt()
        val black = 0xFF000000.toInt()
        val width = result.width
        val height = result.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (result.get(x, y)) black else white
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    fun generateSecretKey(password: CharArray, salt: ByteArray): SecretKey {
        val keyFact = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hmacKey = keyFact.generateSecret(PBEKeySpec(password, salt, 1024, 256))
        return SecretKeySpec(hmacKey.encoded, "AES")
    }

    private fun getHash(bytes: ByteArray): String {
        return byteToHex(MessageDigest.getInstance("SHA256").digest(bytes))
    }

    private fun byteToHex(byteArray: ByteArray): String {
        return String(Hex.encode(byteArray))
    }

    fun hexStringToByteArray(hexString: String): ByteArray {
        val b = ByteArray(hexString.length / 2)
        for (i in b.indices) {
            val index = i * 2
            val v = Integer.parseInt(hexString.substring(index, index + 2), 16)
            b[i] = v.toByte()
        }
        return b
    }

    fun getHash(string: String): String {
        return getHash(string.toByteArray())
    }

    fun createECDSASignatureWithContent(signerPrivateKey: ECPrivateKey, content: ByteArray): ByteArray {
        val ecParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val curve = ecParameterSpec.curve
        val domainParameters = ECDomainParameters(curve, ecParameterSpec.g, ecParameterSpec.n, ecParameterSpec.h)
        val privateKeyParameters = ECPrivateKeyParameters(signerPrivateKey.s, domainParameters)

        val ecdsaSigner = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
        ecdsaSigner.init(true, privateKeyParameters)

        val bigIntegers = ecdsaSigner.generateSignature(content)
        val byteArrayOutputStream = ByteArrayOutputStream()
        try {
            for (bigInteger in bigIntegers) {
                val tempBytes = bigInteger.toByteArray()
                when {
                    tempBytes.size == 31 -> byteArrayOutputStream.write(0)
                    tempBytes.size == 32 -> byteArrayOutputStream.write(tempBytes)
                    else -> byteArrayOutputStream.write(tempBytes, tempBytes.size - 32, 32)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return byteArrayOutputStream.toByteArray()
    }
}