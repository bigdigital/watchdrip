#include <jni.h>
#include "ecdh_lib.h"

JNIEXPORT jbyteArray JNICALL
Java_com_thatguysservice_huami_1xdrip_watch_miband_Firmware_operations_AuthOperations2021_ecdh_1lib_1get_1public_1key(
    JNIEnv *env, jobject obj, jbyteArray private_key){
    jbyte public_key[ECC_PUB_KEY_SIZE];

    jbyte *p_private_key = (*env)->GetByteArrayElements(env, private_key, 0);
    if (p_private_key == NULL) {
        return NULL;
    }
    ecdh_generate_keys((uint8_t *) public_key, (uint8_t *) p_private_key);
    (*env)->ReleaseByteArrayElements(env, private_key, p_private_key, JNI_ABORT); /*free VM private_ec, do not copy back contents */
    jbyteArray ret = (*env)->NewByteArray(env, ECC_PUB_KEY_SIZE);
    if (ret == NULL) {
        return NULL; /* out of memory error thrown */
    }
    (*env)->SetByteArrayRegion(env, ret, 0, ECC_PUB_KEY_SIZE, public_key);  // move from the c structure to the java structure
    return ret;
}


JNIEXPORT jbyteArray JNICALL
Java_com_thatguysservice_huami_1xdrip_watch_miband_Firmware_operations_AuthOperations2021_ecdh_1lib_1get_1shared_1key(
    JNIEnv *env, jobject obj, jbyteArray private_key, jbyteArray public_key){
    jbyte shared_key[ECC_PUB_KEY_SIZE];

    jbyte *p_private_key = (*env)->GetByteArrayElements(env, private_key, 0);
    jbyte *p_public_key = (*env)->GetByteArrayElements(env, public_key, 0);

    if (p_private_key == NULL || p_public_key == NULL) {
        return NULL;
    }

    ecdh_shared_secret((uint8_t *) p_private_key, (uint8_t *) p_public_key, (uint8_t *) shared_key);

    (*env)->ReleaseByteArrayElements(env, private_key, p_private_key, JNI_ABORT); /*free VM private_ec, do not copy back contents */
    (*env)->ReleaseByteArrayElements(env, public_key, p_public_key, JNI_ABORT);   /*free VM public_ec, do not copy back contents */

    jbyteArray ret = (*env)->NewByteArray(env, ECC_PUB_KEY_SIZE);
    if (ret == NULL) {
        return NULL; /* out of memory error thrown */
    }
    (*env)->SetByteArrayRegion(env, ret, 0, ECC_PUB_KEY_SIZE, shared_key);
    return ret;
}
