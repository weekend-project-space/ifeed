<!-- components/MediaAttachment.vue -->
<template>
  <section v-if="url && isSupported" >
    <!-- 媒体卡片 - Google Material Design 风格 -->
    <div class="rounded-lg bg-secondary/5 hover:bg-secondary/10  overflow-hidden transition-colors duration-200">

      <!-- 视频播放器 -->
      <template v-if="isVideo">
        <div class="aspect-video bg-black">
          <video
              :src="url"
              controls
              controlsList="nodownload"
              class="w-full h-full"
          ></video>
        </div>
        <div class="px-4 py-3 border-t border-gray-100">
          <div class="flex items-center gap-2.5">
            <div class="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
              <svg class="w-4 h-4 text-primary" fill="currentColor" viewBox="0 0 20 20">
                <path d="M2 6a2 2 0 012-2h6a2 2 0 012 2v8a2 2 0 01-2 2H4a2 2 0 01-2-2V6zm12.553 1.106A1 1 0 0014 8v4a1 1 0 00.553.894l2 1A1 1 0 0018 13V7a1 1 0 00-1.447-.894l-2 1z"/>
              </svg>
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm text-gray-700 truncate">{{ displayFileName }}</p>
            </div>
          </div>
        </div>
      </template>

      <!-- 音频播放器 -->
      <template v-else-if="isAudio">
        <div class="p-4">
          <div class="flex items-start gap-4">
            <!-- 封面图 + 播放按钮 -->
            <div class="flex-shrink-0 relative group">
              <div
                  v-if="coverImage"
                  class="w-20 h-20 rounded-lg overflow-hidden "
              >
                <img
                    :src="coverImage"
                    :alt="title || '封面'"
                    class="w-full h-full object-cover"
                    @error="handleImageError"
                />
              </div>
              <div
                  v-else
                  class="w-20 h-20 rounded-lg bg-primary/10 flex items-center justify-center"
              >
                <svg class="w-10 h-10 text-primary" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M18 3a1 1 0 00-1.196-.98l-10 2A1 1 0 006 5v9.114A4.369 4.369 0 005 14c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V7.82l8-1.6v5.894A4.37 4.37 0 0015 12c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V3z"/>
                </svg>
              </div>

              <!-- 播放/暂停按钮 - 覆盖在封面上 -->
              <button
                  @click="toggleAudio"
                  class="absolute inset-0 flex items-center justify-center bg-surface-container/30 opacity-0 group-hover:opacity-100 transition-opacity duration-200 rounded-lg focus:outline-none focus:opacity-100"
                  :class="{ 'opacity-100': playing }"
                  aria-label="播放/暂停"
              >
                <!-- 播放图标 -->
                <div v-if="!playing" class="w-12 h-12 rounded-full bg-surface-container/60 hover:bg-surface-container/80 flex items-center justify-center transition-colors">
                  <svg class="w-5 h-5 text-secondary ml-0.5" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M6.3 2.841A1.5 1.5 0 004 4.11V15.89a1.5 1.5 0 002.3 1.269l9.344-5.89a1.5 1.5 0 000-2.538L6.3 2.84z"/>
                  </svg>
                </div>
                <!-- 暂停图标 -->
                <div v-else class="w-12 h-12 rounded-full bg-surface-container/60 hover:bg-surface-container/80 flex items-center justify-center transition-colors">
                  <svg class="w-5 h-5 text-secondary" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M5 4a2 2 0 012-2h1a2 2 0 012 2v12a2 2 0 01-2 2H7a2 2 0 01-2-2V4zm8 0a2 2 0 012-2h1a2 2 0 012 2v12a2 2 0 01-2 2h-1a2 2 0 01-2-2V4z"/>
                  </svg>
                </div>
              </button>
            </div>

            <!-- 音频信息和控制 -->
            <div class="flex-1 min-w-0">
              <!-- 标题和艺术家 -->
              <div class="mb-3">
                <h3 v-if="title" class="text-base font-medium  truncate mb-0.5">
                  {{ title }}
                </h3>
                <p v-if="artist" class="text-sm truncate">
                  {{ artist }}
                </p>
                <p v-else class="text-sm  truncate">
                  {{ displayFileName }}
                </p>
              </div>

              <!-- 时间显示 -->
              <div class="mb-2">
                <div v-if="duration > 0" class="text-xs text-secondary tabular-nums">
                  {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
                </div>
              </div>

              <!-- 进度条 -->
              <div v-if="duration > 0">
                <div class="relative w-full h-1 bg-gray-200 rounded-full overflow-hidden cursor-pointer group" @click="seekAudio">
                  <div
                      class="absolute inset-y-0 left-0 bg-primary rounded-full transition-all duration-100"
                      :style="{ width: progressPercent + '%' }"
                  ></div>
                  <!-- 进度指示器 -->
                  <div
                      class="absolute top-1/2 -translate-y-1/2 w-3 h-3 bg-primary rounded-full opacity-0 group-hover:opacity-100 transition-opacity"
                      :style="{ left: `calc(${progressPercent}% - 6px)` }"
                  ></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 隐藏的音频元素 -->
        <audio
            ref="audioRef"
            :src="url"
            @timeupdate="onTimeUpdate"
            @loadedmetadata="onLoadedMetadata"
            @ended="onEnded"
            @play="onPlay"
            @pause="onPause"
            class="hidden"
        ></audio>
      </template>

      <!-- 图片展示 -->
      <template v-else-if="isImage">
        <div class="bg-gray-50">
          <img
              :src="url"
              :alt="displayFileName"
              class="w-full max-h-[500px] object-contain"
              loading="lazy"
          />
        </div>
        <div class="px-4 py-3 border-t border-gray-100">
          <div class="flex items-center gap-2.5">
            <div class="w-8 h-8 rounded-full bg-secondary/10 flex items-center justify-center flex-shrink-0">
              <svg class="w-4 h-4 text-secondary" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z" clip-rule="evenodd"/>
              </svg>
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm text-gray-700 truncate">{{ displayFileName }}</p>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 不支持的文件类型 - 简洁链接样式 -->
    <a
        v-if="!isSupported && url"
        :href="url"
        target="_blank"
        rel="noopener noreferrer"
        class="inline-flex items-center gap-2 px-4 py-2.5 rounded-full text-sm font-medium text-primary hover:bg-primary/5 active:bg-primary/10 transition-colors duration-150"
    >
      <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
        <path fill-rule="evenodd" d="M8 4a3 3 0 00-3 3v4a5 5 0 0010 0V7a1 1 0 112 0v4a7 7 0 11-14 0V7a5 5 0 0110 0v4a3 3 0 11-6 0V7a1 1 0 012 0v4a1 1 0 102 0V7a3 3 0 00-3-3z" clip-rule="evenodd"/>
      </svg>
      <span>查看附件</span>
    </a>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted, onBeforeUnmount } from 'vue';

interface Props {
  url: string | null;
  type?: string | null;      // MIME type
  title?: string | null;     // 音频/视频标题
  artist?: string | null;    // 艺术家/作者
  coverImage?: string | null; // 封面图片
}

const props = withDefaults(defineProps<Props>(), {
  type: null,
  title: null,
  artist: null,
  coverImage: null
});

// 音频状态
const audioRef = ref<HTMLAudioElement | null>(null);
const playing = ref(false);
const currentTime = ref(0);
const duration = ref(0);
const imageError = ref(false);

// 媒体类型判断
const mimeType = computed(() => props.type || '');
const urlLower = computed(() => props.url?.toLowerCase() || '');

const isAudio = computed(() =>
    mimeType.value.startsWith('audio') ||
    /\.(mp3|wav|aac|m4a|ogg|flac)$/i.test(urlLower.value)
);

const isVideo = computed(() =>
    mimeType.value.startsWith('video') ||
    /\.(mp4|mov|webm|mkv|avi)$/i.test(urlLower.value)
);

const isImage = computed(() =>
    mimeType.value.startsWith('image') ||
    /\.(jpg|jpeg|png|gif|webp|svg|bmp)$/i.test(urlLower.value)
);

const isSupported = computed(() =>
    isAudio.value || isVideo.value || isImage.value
);

// 文件名提取
const displayFileName = computed(() =>
    props.url?.split('/').pop()?.split('?')[0] || '未知文件'
);

// 进度百分比
const progressPercent = computed(() =>
    duration.value > 0 ? (currentTime.value / duration.value) * 100 : 0
);

// 实际显示的封面（如果图片加载失败则隐藏）
const coverImage = computed(() =>
    imageError.value ? null : props.coverImage
);

// 音频控制
const toggleAudio = () => {
  if (!audioRef.value) return;

  if (playing.value) {
    audioRef.value.pause();
  } else {
    audioRef.value.play();
  }
};

// 进度条点击跳转
const seekAudio = (event: MouseEvent) => {
  if (!audioRef.value || duration.value <= 0) return;

  const progressBar = event.currentTarget as HTMLElement;
  const rect = progressBar.getBoundingClientRect();
  const clickX = event.clientX - rect.left;
  const percent = clickX / rect.width;

  audioRef.value.currentTime = percent * duration.value;
};

// 音频事件处理
const onTimeUpdate = () => {
  if (audioRef.value) {
    currentTime.value = audioRef.value.currentTime;
  }
};

const onLoadedMetadata = () => {
  if (audioRef.value) {
    duration.value = audioRef.value.duration;
    updateMediaSession();
  }
};

const onEnded = () => {
  playing.value = false;
  currentTime.value = 0;
};

const onPlay = () => {
  playing.value = true;
};

const onPause = () => {
  playing.value = false;
};

// 图片加载错误处理
const handleImageError = () => {
  imageError.value = true;
};

// 时间格式化 (秒 -> MM:SS)
const formatTime = (seconds: number): string => {
  if (!isFinite(seconds)) return '0:00';
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs.toString().padStart(2, '0')}`;
};

// 更新浏览器媒体会话 (Media Session API)
const updateMediaSession = () => {
  if (!('mediaSession' in navigator) || !isAudio.value) return;

  try {
    navigator.mediaSession.metadata = new MediaMetadata({
      title: props.title || displayFileName.value,
      artist: props.artist || '未知艺术家',
      album: '',
      artwork: props.coverImage ? [
        { src: props.coverImage, sizes: '96x96', type: 'image/jpeg' },
        { src: props.coverImage, sizes: '128x128', type: 'image/jpeg' },
        { src: props.coverImage, sizes: '192x192', type: 'image/jpeg' },
        { src: props.coverImage, sizes: '256x256', type: 'image/jpeg' },
        { src: props.coverImage, sizes: '384x384', type: 'image/jpeg' },
        { src: props.coverImage, sizes: '512x512', type: 'image/jpeg' },
      ] : []
    });

    // 设置播放控制处理器
    navigator.mediaSession.setActionHandler('play', () => {
      audioRef.value?.play();
    });

    navigator.mediaSession.setActionHandler('pause', () => {
      audioRef.value?.pause();
    });

    navigator.mediaSession.setActionHandler('seekbackward', () => {
      if (audioRef.value) {
        audioRef.value.currentTime = Math.max(0, audioRef.value.currentTime - 10);
      }
    });

    navigator.mediaSession.setActionHandler('seekforward', () => {
      if (audioRef.value && duration.value > 0) {
        audioRef.value.currentTime = Math.min(duration.value, audioRef.value.currentTime + 10);
      }
    });

    navigator.mediaSession.setActionHandler('seekto', (details) => {
      if (audioRef.value && details.seekTime != null) {
        audioRef.value.currentTime = details.seekTime;
      }
    });
  } catch (err) {
    console.warn('Media Session API not fully supported', err);
  }
};

// 监听 props 变化更新 Media Session
watch(
    () => [props.title, props.artist, props.coverImage, duration.value],
    () => {
      if (isAudio.value && duration.value > 0) {
        updateMediaSession();
      }
    }
);

// 清理 Media Session
onBeforeUnmount(() => {
  if ('mediaSession' in navigator) {
    try {
      navigator.mediaSession.metadata = null;
      navigator.mediaSession.setActionHandler('play', null);
      navigator.mediaSession.setActionHandler('pause', null);
      navigator.mediaSession.setActionHandler('seekbackward', null);
      navigator.mediaSession.setActionHandler('seekforward', null);
      navigator.mediaSession.setActionHandler('seekto', null);
    } catch (err) {
      console.warn('Failed to clear Media Session', err);
    }
  }
});
</script>