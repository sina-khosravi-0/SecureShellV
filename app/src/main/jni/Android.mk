LOCAL_PATH := $(call my-dir)

				include $(CLEAR_VARS)
				LOCAL_MODULE := HelloLibrary
				#VisualGDBAndroid: AutoUpdateSourcesInNextLine
				LOCAL_SRC_FILES := hello.c
				include $(BUILD_SHARED_LIBRARY)