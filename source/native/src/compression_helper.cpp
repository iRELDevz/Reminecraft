#include <jni.h>
#include <vector>
#include <cstring>
#include <windows.h>
#include <compressapi.h>

extern "C" {

JNIEXPORT jbyteArray JNICALL Java_org_purpurmc_purpur_reminecraft_NativeCompression_compress(
    JNIEnv* env, jclass clazz, jbyteArray input, jint level) {

    if (!input) return nullptr;

    jsize input_len = env->GetArrayLength(input);
    jbyte* input_data = env->GetByteArrayElements(input, nullptr);
    if (!input_data) return nullptr;

    COMPRESSOR_HANDLE compressor = NULL;
    if (!CreateCompressor(COMPRESS_ALGORITHM_MSZIP, NULL, &compressor)) {
        env->ReleaseByteArrayElements(input, input_data, JNI_ABORT);
        return nullptr;
    }

    SIZE_T compressed_size = 0;
    Compress(compressor, input_data, (SIZE_T)input_len, nullptr, 0, &compressed_size);

    std::vector<BYTE> compressed_buffer(compressed_size);
    BOOL ok = Compress(compressor, input_data, (SIZE_T)input_len,
                       compressed_buffer.data(), compressed_size, &compressed_size);

    CloseCompressor(compressor);
    env->ReleaseByteArrayElements(input, input_data, JNI_ABORT);

    if (!ok) return nullptr;

    jbyteArray output = env->NewByteArray((jsize)compressed_size);
    if (output) {
        env->SetByteArrayRegion(output, 0, (jsize)compressed_size,
                                reinterpret_cast<const jbyte*>(compressed_buffer.data()));
    }
    return output;
}

JNIEXPORT jbyteArray JNICALL Java_org_purpurmc_purpur_reminecraft_NativeCompression_decompress(
    JNIEnv* env, jclass clazz, jbyteArray input, jint original_len) {

    if (!input || original_len <= 0) return nullptr;

    jsize input_len = env->GetArrayLength(input);
    jbyte* input_data = env->GetByteArrayElements(input, nullptr);
    if (!input_data) return nullptr;

    DECOMPRESSOR_HANDLE decompressor = NULL;
    if (!CreateDecompressor(COMPRESS_ALGORITHM_MSZIP, NULL, &decompressor)) {
        env->ReleaseByteArrayElements(input, input_data, JNI_ABORT);
        return nullptr;
    }

    std::vector<BYTE> decompressed_buffer((size_t)original_len);
    SIZE_T decompressed_size = (SIZE_T)original_len;
    BOOL ok = Decompress(decompressor, input_data, (SIZE_T)input_len,
                         decompressed_buffer.data(), decompressed_size, &decompressed_size);

    CloseDecompressor(decompressor);
    env->ReleaseByteArrayElements(input, input_data, JNI_ABORT);

    if (!ok) return nullptr;

    jbyteArray output = env->NewByteArray((jsize)decompressed_size);
    if (output) {
        env->SetByteArrayRegion(output, 0, (jsize)decompressed_size,
                                reinterpret_cast<const jbyte*>(decompressed_buffer.data()));
    }
    return output;
}

}
