#include <jni.h>
#include <string>
#include <android/log.h>
#include <vector>

#define LOG_TAG "ByeDPI_Native"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// IP ve TCP başlık boyutları
const int IPV4_HEADER_MIN_LEN = 20;
const int TCP_HEADER_MIN_LEN = 20;

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_example_byedpi_DpiBypassService_processPacket(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray packetArray,
        jint length) {
    
    jbyte* packetBytes = env->GetByteArrayElements(packetArray, nullptr);
    uint8_t* packet = reinterpret_cast<uint8_t*>(packetBytes);

    // 1. Basit bir IPv4 ve TCP kontrolü
    // IPv4 (Versiyon 4) ve TCP Protokolü (6) kontrolü
    if (length > IPV4_HEADER_MIN_LEN && (packet[0] >> 4) == 4 && packet[9] == 6) {
        
        int ipHeaderLen = (packet[0] & 0x0F) * 4;
        uint8_t* tcpHeader = packet + ipHeaderLen;
        
        if (length > ipHeaderLen + TCP_HEADER_MIN_LEN) {
            int tcpHeaderLen = ((tcpHeader[12] >> 4) & 0x0F) * 4;
            uint8_t* payload = tcpHeader + tcpHeaderLen;
            int payloadLen = length - ipHeaderLen - tcpHeaderLen;

            // 2. TLS "ClientHello" Paketi mi? (DPI'ın okuduğu paket)
            // Handshake (22), Version (0x03, 0x01/0x02/0x03), ClientHello (0x01)
            if (payloadLen > 5 && payload[0] == 0x16 && payload[1] == 0x03 && payload[5] == 0x01) {
                LOGD("TLS ClientHello Tespit Edildi! Boyut: %d", payloadLen);
                
                // --- GOODBYEDPI MANTIĞI BURADA DEVREYE GİRER ---
                // Bu noktada paket doğrudan yollanmaz. 
                // Hedef sunucuya gönderilirken bu payload'un ilk 2 byte'ı (veya SNI'a kadar olan kısmı)
                // ayrı bir TCP paketi olarak gönderilir, geri kalanı ayrı bir pakette gönderilir.
                // Buna "TCP Split" (TCP Parçalama) denir.
                
                // Not: Android VpnService tek taraflı bir tüneldir. Bu paketi parçalayıp
                // internete doğrudan ham (raw) olarak salamayız (Root olmadan yasaktır).
                // Gerçek bir uygulamada burada paket içindeki IP'ye bir 'Socket' açılır,
                // ClientHello verisi iki parça halinde "write()" edilir.
            }
        }
    }
    
    // Şimdilik paketi dokunmadan geri döndürüyoruz
    env->ReleaseByteArrayElements(packetArray, packetBytes, 0);
    return nullptr;
}
