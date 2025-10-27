package kr.jiasoft.hiteen.feature.ad.app

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URL
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.*

@Component
class AdmobVerifier(
    private val objectMapper: ObjectMapper,
    @param:Value("\${admob.key-url}") private val googleKeyUrl: String,
) {


    /**
     * AdMob 서버 서명 검증
     */
    fun verifySignature(
        params: Map<String, String>,
        signature: String?,
        keyId: String?
    ): Boolean {
        if (signature.isNullOrBlank() || keyId.isNullOrBlank()) return false

        return try {
            val publicKey = fetchGooglePublicKey(keyId)

            // Canonical query 생성
            val canonicalQuery = params
                .filterKeys { it != "signature" && it != "key_id" }
                .toSortedMap()
                .map { "${it.key}=${it.value}" }
                .joinToString("&")

            // ✅ URL-safe Base64 decoding
            val signatureBytes = Base64.getUrlDecoder().decode(signature)

            // ✅ AdMob은 EC 서명 → ECDSA 사용
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(publicKey)
            verifier.update(canonicalQuery.toByteArray(Charsets.UTF_8))

            val result = verifier.verify(signatureBytes)
            println("✅ Signature verified = $result")
            result
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    /**
     * Google Public Key fetch & decode
     */
    private fun fetchGooglePublicKey(keyId: String): PublicKey {
        println("🔑 Fetching AdMob public key list: $googleKeyUrl")

        val json = URL(googleKeyUrl).readText()
        val root = objectMapper.readTree(json)

        // ✅ JSON 구조: { "keys": [ { "keyId": 3335741209, "pem": "-----BEGIN PUBLIC KEY-----...", ... } ] }
        val keys = root["keys"]
        val matchedKey = keys.firstOrNull { it["keyId"].asText() == keyId }
            ?: throw IllegalArgumentException("❌ key_id($keyId)에 해당하는 공개키를 찾을 수 없습니다.")

        val pem = matchedKey["pem"].asText()
        val cleanedKey = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")

        val keyBytes = Base64.getDecoder().decode(cleanedKey)
        val spec = X509EncodedKeySpec(keyBytes)

        return KeyFactory.getInstance("EC").generatePublic(spec) // ✅ EC 키 알고리즘 사용
    }

}
