LOCAL_PATH := $(call my-dir)  
include $(CLEAR_VARS)  
LOCAL_MODULE := function 

UNRAR_FILES = unrar/filestr.cpp unrar/recvol.cpp unrar/rs.cpp unrar/scantree.cpp
LIB_FILES = unrar/filestr.cpp unrar/scantree.cpp unrar/dll.cpp

RAR_FILES = unrar/consio.cpp unrar/rar.cpp unrar/strlist.cpp unrar/strfn.cpp unrar/pathfn.cpp unrar/savepos.cpp unrar/smallfn.cpp unrar/global.cpp unrar/file.cpp unrar/filefn.cpp unrar/filcreat.cpp \
	unrar/archive.cpp unrar/arcread.cpp unrar/unicode.cpp unrar/system.cpp unrar/isnt.cpp unrar/crypt.cpp unrar/crc.cpp unrar/rawread.cpp unrar/encname.cpp \
	unrar/resource.cpp unrar/match.cpp unrar/timefn.cpp unrar/rdwrfn.cpp unrar/options.cpp unrar/ulinks.cpp unrar/errhnd.cpp unrar/rarvm.cpp \
	unrar/rijndael.cpp unrar/getbits.cpp unrar/sha1.cpp unrar/extinfo.cpp unrar/extract.cpp unrar/volume.cpp unrar/list.cpp unrar/find.cpp unrar/unpack.cpp unrar/cmddata.cpp

ZIPA_FILES = zipalign/ZipAlign.cpp zipalign/ZipEntry.cpp zipalign/ZipFile.cpp zipalign/android/native/src/utils/SharedBuffer.cpp zipalign/android/native/src/utils/ZipUtils.cpp zipalign/android/native/src/utils/VectorImpl.cpp

LOCAL_SRC_FILES := ELFHash.cpp mainfunction.cpp zlibmgr.cpp rarext.cpp oat2dex.cpp parse_elf.cpp odex2dex.cpp zipa.cpp $(RAR_FILES) $(LIB_FILES) $(ZIPA_FILES)

LOCAL_LDLIBS := -llog -lz

LOCAL_C_INCLUDES += $(LOCAL_PATH)/zipalign \
$(LOCAL_PATH)/zipalign/android/base/include \
$(LOCAL_PATH)/zipalign/android/core/include \
$(LOCAL_PATH)/zipalign/android/native/include

LOCAL_CFLAGS := -DSILENT -DRARDLL -I$(LOCAL_PATH)/rar -fexceptions -Os -ffunction-sections -fdata-sections -fvisibility=hidden -w -Wl,--gc-sections

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    := FileHelper
LOCAL_SRC_FILES := android_os_FileHelper.cpp

include $(BUILD_SHARED_LIBRARY)
