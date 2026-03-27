#include <jni.h>
#include <android/log.h>
#include <string>
#include <cerrno>
#include <cstdio>
#include <cstring>
#include <sys/stat.h>

#include "mobi.h"

namespace {
    const char *TAG = "MobiNative";
    const char *EPUB_MIMETYPE = "application/epub+zip";
    const char *EPUB_CONTAINER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n"
        "  <rootfiles>\n"
        "    <rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n"
        "  </rootfiles>\n"
        "</container>\n";

    void throwJava(JNIEnv *env, const std::string &message) {
        jclass exClass = env->FindClass("java/lang/RuntimeException");
        if (exClass) {
            env->ThrowNew(exClass, message.c_str());
        }
    }

    std::string toString(JNIEnv *env, jstring value) {
        if (!value) return {};
        const char *chars = env->GetStringUTFChars(value, nullptr);
        std::string out = chars ? chars : "";
        if (chars) {
            env->ReleaseStringUTFChars(value, chars);
        }
        return out;
    }

    bool ensureDir(const std::string &path) {
        if (path.empty()) return false;
        if (mkdir(path.c_str(), 0755) == 0) return true;
        if (errno == EEXIST) return true;
        __android_log_print(ANDROID_LOG_ERROR, TAG, "mkdir failed: %s (%s)", path.c_str(), strerror(errno));
        return false;
    }

    bool writeFile(const std::string &path, const unsigned char *data, size_t size) {
        FILE *file = fopen(path.c_str(), "wb");
        if (!file) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "fopen failed: %s (%s)", path.c_str(), strerror(errno));
            return false;
        }
        if (size > 0 && data) {
            const size_t written = fwrite(data, 1, size, file);
            fclose(file);
            return written == size;
        }
        fclose(file);
        return true;
    }

    std::string joinPath(const std::string &base, const std::string &child) {
        if (base.empty()) return child;
        if (base.back() == '/') return base + child;
        return base + "/" + child;
    }

    bool writeEpubStructure(const MOBIRawml *rawml, const std::string &outputDir) {
        if (!ensureDir(outputDir)) {
            return false;
        }
        const std::string metaInfDir = joinPath(outputDir, "META-INF");
        const std::string oebpsDir = joinPath(outputDir, "OEBPS");
        if (!ensureDir(metaInfDir) || !ensureDir(oebpsDir)) {
            return false;
        }

        const std::string mimetypePath = joinPath(outputDir, "mimetype");
        if (!writeFile(mimetypePath, reinterpret_cast<const unsigned char *>(EPUB_MIMETYPE), strlen(EPUB_MIMETYPE))) {
            return false;
        }

        const std::string containerPath = joinPath(metaInfDir, "container.xml");
        if (!writeFile(containerPath, reinterpret_cast<const unsigned char *>(EPUB_CONTAINER), strlen(EPUB_CONTAINER))) {
            return false;
        }

        char partName[256];
        if (rawml->markup) {
            MOBIPart *curr = rawml->markup;
            while (curr) {
                MOBIFileMeta fileMeta = mobi_get_filemeta_by_type(curr->type);
                snprintf(partName, sizeof(partName), "part%05zu.%s", curr->uid, fileMeta.extension);
                const std::string path = joinPath(oebpsDir, partName);
                if (!writeFile(path, curr->data, curr->size)) {
                    return false;
                }
                curr = curr->next;
            }
        }

        if (rawml->flow) {
            MOBIPart *curr = rawml->flow;
            curr = curr->next;
            while (curr) {
                MOBIFileMeta fileMeta = mobi_get_filemeta_by_type(curr->type);
                snprintf(partName, sizeof(partName), "flow%05zu.%s", curr->uid, fileMeta.extension);
                const std::string path = joinPath(oebpsDir, partName);
                if (!writeFile(path, curr->data, curr->size)) {
                    return false;
                }
                curr = curr->next;
            }
        }

        if (rawml->resources) {
            MOBIPart *curr = rawml->resources;
            while (curr) {
                if (curr->size > 0) {
                    MOBIFileMeta fileMeta = mobi_get_filemeta_by_type(curr->type);
                    if (fileMeta.type == T_OPF) {
                        snprintf(partName, sizeof(partName), "content.opf");
                    } else {
                        snprintf(partName, sizeof(partName), "resource%05zu.%s", curr->uid, fileMeta.extension);
                    }
                    const std::string path = joinPath(oebpsDir, partName);
                    if (!writeFile(path, curr->data, curr->size)) {
                        return false;
                    }
                }
                curr = curr->next;
            }
        }

        return true;
    }
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_dyu_ereader_data_format_mobi_MobiNative_extractToEpubDir(
    JNIEnv *env,
    jobject /* this */,
    jstring inputPath,
    jstring outputDir
) {
    const std::string input = toString(env, inputPath);
    const std::string output = toString(env, outputDir);

    if (input.empty() || output.empty()) {
        throwJava(env, "Invalid MOBI path");
        return nullptr;
    }

    MOBIData *m = mobi_init();
    if (!m) {
        throwJava(env, "libmobi init failed");
        return nullptr;
    }

    MOBI_RET mobiRet = mobi_load_filename(m, input.c_str());
    if (mobiRet != MOBI_SUCCESS) {
        std::string msg = std::string("MOBI load failed (code ") + std::to_string(mobiRet) + ")";
        mobi_free(m);
        throwJava(env, msg);
        return nullptr;
    }

    if (mobi_is_encrypted(m)) {
        mobi_free(m);
        throwJava(env, "MOBI file is encrypted (DRM)");
        return nullptr;
    }

    if (mobi_is_replica(m)) {
        mobi_free(m);
        throwJava(env, "Print Replica MOBI is not supported for conversion");
        return nullptr;
    }

    MOBIRawml *rawml = mobi_init_rawml(m);
    if (!rawml) {
        mobi_free(m);
        throwJava(env, "MOBI rawml init failed");
        return nullptr;
    }

    mobiRet = mobi_parse_rawml(rawml, m);
    if (mobiRet != MOBI_SUCCESS) {
        std::string msg = std::string("MOBI rawml parse failed (code ") + std::to_string(mobiRet) + ")";
        mobi_free_rawml(rawml);
        mobi_free(m);
        throwJava(env, msg);
        return nullptr;
    }

    const bool ok = writeEpubStructure(rawml, output);
    mobi_free_rawml(rawml);
    mobi_free(m);

    if (!ok) {
        throwJava(env, "Failed to write EPUB structure");
        return nullptr;
    }

    return env->NewStringUTF(output.c_str());
}
