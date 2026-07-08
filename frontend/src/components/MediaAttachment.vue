<!-- components/MediaAttachment.vue -->
<template>
  <section v-if="url">
    <!-- 支持的媒体类型 -->
    <div v-if="isSupported" class="rounded-lg bg-secondary/5 hover:bg-secondary/10 dark:bg-secondary/10 dark:hover:bg-secondary/20 overflow-hidden transition-colors duration-200">

      <!-- 视频播放器 -->
      <template v-if="isVideo">
        <div class="aspect-video bg-black">
          <video
              :src="url"
              controls
              controlsList="nodownload"
              class="w-full h-full"
              @error="onVideoError"
          ></video>
        </div>
        <div class="px-4 py-3 border-t border-gray-100 dark:border-gray-700">
          <div class="flex items-center gap-2.5">
            <div class="w-8 h-8 rounded-full bg-primary/10 dark:bg-primary/20 flex items-center justify-center flex-shrink-0">
              <svg class="w-4 h-4 text-primary" fill="currentColor" viewBox="0 0 20 20">
                <path
                    d="M2 6a2 2 0 012-2h6a2 2 0 012 2v8a2 2 0 01-2 2H4a2 2 0 01-2-2V6zm12.553 1.106A1 1 0 0014 8v4a1 1 0 00.553.894l2 1A1 1 0 0018 13V7a1 1 0 00-1.447-.894l-2 1z"/>
              </svg>
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm text-gray-700 dark:text-gray-300 truncate">{{ displayFileName }}</p>
            </div>
          </div>
        </div>
      </template>

      <!-- 音频播放器 -->
      <template v-else-if="isAudio">
        <!-- 内联模式 -->
        <div v-if="mode === 'inline'" class="p-4">
          <div class="flex items-start gap-4">
            <!-- 封面图 + 播放按钮 -->
            <div class="flex-shrink-0 relative group">
              <div
                  v-if="coverImage"
                  class="w-24 h-24 rounded-lg overflow-hidden"
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
                  class="w-24 h-24 rounded-lg bg-primary/10 dark:bg-primary/20 flex items-center justify-center"
              >
                <svg class="w-10 h-10 text-primary" fill="currentColor" viewBox="0 0 20 20">
                  <path
                      d="M18 3a1 1 0 00-1.196-.98l-10 2A1 1 0 006 5v9.114A4.369 4.369 0 005 14c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V7.82l8-1.6v5.894A4.37 4.37 0 0015 12c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V3z"/>
                </svg>
              </div>

              <!-- 播放/暂停按钮 - 覆盖在封面上 -->
              <button
                  @click="toggleAudio"
                  class="absolute inset-0 flex items-center justify-center bg-black/30 opacity-0 group-hover:opacity-100 transition-opacity duration-200 rounded-lg focus:outline-none focus:opacity-100"
                  :class="{ 'opacity-100': isPlaying }"
                  aria-label="播放/暂停"
              >
                <div v-if="!isPlaying"
                     class="w-12 h-12 rounded-full bg-white/60 hover:bg-white/80 flex items-center justify-center transition-colors">
                  <svg class="w-5 h-5 text-gray-900 ml-0.5" fill="currentColor" viewBox="0 0 20 20">
                    <path
                        d="M6.3 2.841A1.5 1.5 0 004 4.11V15.89a1.5 1.5 0 002.3 1.269l9.344-5.89a1.5 1.5 0 000-2.538L6.3 2.84z"/>
                  </svg>
                </div>
                <div v-else
                     class="w-12 h-12 rounded-full bg-white/60 hover:bg-white/80 flex items-center justify-center transition-colors">
                  <svg class="w-5 h-5 text-gray-900" fill="currentColor" viewBox="0 0 20 20">
                    <path
                        d="M5 4a2 2 0 012-2h1a2 2 0 012 2v12a2 2 0 01-2 2H7a2 2 0 01-2-2V4zm8 0a2 2 0 012-2h1a2 2 0 012 2v12a2 2 0 01-2 2h-1a2 2 0 01-2-2V4z"/>
                  </svg>
                </div>
              </button>
            </div>

            <!-- 音频信息和控制 -->
            <div class="flex-1 min-w-0">
              <div class="mb-2">
                <h3 v-if="title" class="text-base font-medium text-gray-900 dark:text-gray-100 truncate mb-0.5">
                  {{ title }}
                </h3>
                <p v-if="artist" class="text-sm text-gray-600 dark:text-gray-400 truncate">
                  {{ artist }}
                </p>
                <p v-else class="text-sm text-gray-600 dark:text-gray-400 truncate">
                  {{ displayFileName }}
                </p>
              </div>

              <div class="mb-2">
                <div v-if="duration > 0" class="text-xs text-gray-500 dark:text-gray-400 tabular-nums">
                  {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
                </div>
              </div>

              <div v-if="duration > 0" class="relative py-2 cursor-pointer group" @click="seekAudio">
                <div class="relative w-full h-1 bg-gray-300 dark:bg-gray-600 rounded-full">
                  <div
                      class="absolute inset-y-0 left-0 bg-primary dark:bg-primary rounded-full transition-all duration-100"
                      :style="{ width: progressPercent + '%' }"
                  ></div>
                </div>
                <div
                    class="absolute top-1/2 -translate-y-1/2 w-4 h-4 bg-white dark:bg-gray-200 border-2 border-primary dark:border-primary rounded-full shadow-md transition-opacity pointer-events-none"
                    :class="isPlaying ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'"
                    :style="{ left: `calc(${progressPercent}% - 8px)` }"
                ></div>
              </div>

              <div v-if="audioError" class="text-xs text-red-600 dark:text-red-400 mt-1">
                音频加载失败
              </div>
            </div>
          </div>
        </div>

        <!-- 全局播放器模式 - 简洁卡片 + 添加到播放列表 -->
        <div v-else class="p-4">
          <div class="flex items-center gap-4">
            <!-- 封面缩略图 -->
            <div class="flex-shrink-0">
              <div
                  v-if="coverImage"
                  class="w-16 h-16 rounded-lg overflow-hidden"
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
                  class="w-16 h-16 rounded-lg bg-primary/10 dark:bg-primary/20 flex items-center justify-center"
              >
                <svg class="w-8 h-8 text-primary" fill="currentColor" viewBox="0 0 20 20">
                  <path
                      d="M18 3a1 1 0 00-1.196-.98l-10 2A1 1 0 006 5v9.114A4.369 4.369 0 005 14c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V7.82l8-1.6v5.894A4.37 4.37 0 0015 12c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V3z"/>
                </svg>
              </div>
            </div>

            <!-- 音频信息 -->
            <div class="flex-1 min-w-0">
              <h3 v-if="title" class="text-sm font-medium text-gray-900 dark:text-gray-100 truncate mb-0.5">
                {{ title }}
              </h3>
              <p v-if="artist" class="text-xs text-gray-600 dark:text-gray-400 truncate">
                {{ artist }}
              </p>
              <p v-else class="text-xs text-gray-600 dark:text-gray-400 truncate">
                {{ displayFileName }}
              </p>
              <p v-if="duration > 0" class="text-xs text-gray-500 dark:text-gray-500 mt-1 tabular-nums">
                {{ formatTime(duration) }}
              </p>
            </div>

            <!-- 操作按钮 -->
            <div class="flex items-center gap-2">
              <!-- 播放按钮 -->
              <button
                  @click="playInGlobalPlayer"
                  class="w-10 h-10 rounded-full bg-primary hover:bg-primary/90 active:bg-primary/80 flex items-center justify-center transition-colors focus:outline-none focus:ring-2 focus:ring-primary/50"
                  :class="{ 'bg-primary/80': isCurrentTrack }"
                  aria-label="播放"
              >
                <svg v-if="!isCurrentTrack || !isPlaying" class="w-5 h-5 text-white ml-0.5" fill="currentColor" viewBox="0 0 20 20">
                  <path
                      d="M6.3 2.841A1.5 1.5 0 004 4.11V15.89a1.5 1.5 0 002.3 1.269l9.344-5.89a1.5 1.5 0 000-2.538L6.3 2.84z"/>
                </svg>
                <svg v-else class="w-5 h-5 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M6 4h2.5v12H6V4zm5.5 0H14v12h-2.5V4z"/>
                </svg>
              </button>

              <!-- 添加到播放列表按钮 -->
              <button
                  @click="addToPlaylist"
                  class="w-10 h-10 rounded-full bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 flex items-center justify-center transition-colors focus:outline-none focus:ring-2 focus:ring-gray-300 dark:focus:ring-gray-600"
                  :class="{ 'bg-primary/10 dark:bg-primary/20': isInPlaylist }"
                  aria-label="添加到播放列表"
              >
                <svg v-if="!isInPlaylist" class="w-5 h-5 text-gray-700 dark:text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                </svg>
                <svg v-else class="w-5 h-5 text-primary" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                </svg>
              </button>
            </div>
          </div>

          <div v-if="audioError" class="text-xs text-red-600 dark:text-red-400 mt-2">
            音频加载失败
          </div>
        </div>

        <!-- 隐藏的音频元素 (仅内联模式使用) -->
        <audio
            v-if="mode === 'inline'"
            ref="audioRef"
            :src="url"
            @timeupdate="onTimeUpdate"
            @loadedmetadata="onLoadedMetadata"
            @ended="onEnded"
            @play="onPlay"
            @pause="onPause"
            @error="onAudioError"
            class="hidden"
        ></audio>
      </template>

      <!-- 图片展示 -->
      <template v-else-if="isImage">
        <div class="bg-gray-50 dark:bg-gray-800">
          <img
              :src="url"
              :alt="displayFileName"
              class="w-full max-h-[500px] object-contain"
              loading="lazy"
          />
        </div>
        <div class="px-4 py-3 border-t border-gray-100 dark:border-gray-700">
          <div class="flex items-center gap-2.5">
            <div class="w-8 h-8 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center flex-shrink-0">
              <svg class="w-4 h-4 text-gray-600 dark:text-gray-400" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd"
                      d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z"
                      clip-rule="evenodd"/>
              </svg>
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-sm text-gray-700 dark:text-gray-300 truncate">{{ displayFileName }}</p>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 不支持的文件类型 -->
    <a
        v-else
        :href="url"
        target="_blank"
        rel="noopener noreferrer"
        class="inline-flex items-center gap-2 px-4 py-2.5 rounded-full text-sm font-medium text-primary hover:bg-primary/5 dark:hover:bg-primary/10 active:bg-primary/10 dark:active:bg-primary/20 transition-colors duration-150"
    >
      <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
        <path fill-rule="evenodd"
              d="M8 4a3 3 0 00-3 3v4a5 5 0 0010 0V7a1 1 0 112 0v4a7 7 0 11-14 0V7a5 5 0 0110 0v4a3 3 0 11-6 0V7a1 1 0 012 0v4a1 1 0 102 0V7a3 3 0 00-3-3z"
              clip-rule="evenodd"/>
      </svg>
      <span>查看附件</span>
    </a>
  </section>
</template>

<script setup lang="ts">
import {computed, ref, watch, onBeforeUnmount, inject} from 'vue';

interface Props {
  url: string | null;
  type?: string | null;
  title?: string | null;
  artist?: string | null;
  coverImage?: string | null;
  mode?: 'inline' | 'global'; // 新增：模式选择
  trackId?: string | null; // 新增：用于全局播放器追踪
}

const props = withDefaults(defineProps<Props>(), {
  type: null,
  title: null,
  artist: null,
  coverImage: null,
  mode: 'inline',
  trackId: null
});

// 注入全局播放器（如果存在）
const globalPlayer = inject<any>('globalAudioPlayer', null);

// 内联模式状态
const audioRef = ref<HTMLAudioElement | null>(null);
const playing = ref(false);
const currentTime = ref(0);
const duration = ref(0);
const imageError = ref(false);
const audioError = ref(false);

// 全局播放器状态
const isCurrentTrack = computed(() => {
  if (!globalPlayer || !props.trackId) return false;
  return globalPlayer.currentTrackId.value === props.trackId;
});

const isPlaying = computed(() => {
  if (props.mode === 'inline') {
    return playing.value;
  }
  return isCurrentTrack.value && globalPlayer?.isPlaying.value;
});

const isInPlaylist = computed(() => {
  if (!globalPlayer || !props.trackId) return false;
  return globalPlayer.playlist.value.some((track: any) => track.id === props.trackId);
});

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

const displayFileName = computed(() =>
    props.url?.split('/').pop()?.split('?')[0] || '未知文件'
);

const progressPercent = computed(() =>
    duration.value > 0 ? (currentTime.value / duration.value) * 100 : 0
);

const coverImage = computed(() =>
    imageError.value ? null : props.coverImage
);

// 内联模式控制
const toggleAudio = () => {
  if (!audioRef.value) return;
  if (playing.value) {
    audioRef.value.pause();
  } else {
    audioRef.value.play();
  }
};

const seekAudio = (event: MouseEvent) => {
  if (!audioRef.value || duration.value <= 0) return;
  const progressBar = event.currentTarget as HTMLElement;
  const rect = progressBar.getBoundingClientRect();
  const clickX = event.clientX - rect.left;
  const percent = clickX / rect.width;
  audioRef.value.currentTime = percent * duration.value;
};

// 全局播放器控制
const playInGlobalPlayer = () => {
  if (!globalPlayer) return;

  const track = {
    id: props.trackId || `track-${Date.now()}`,
    url: props.url,
    title: props.title || displayFileName.value,
    artist: props.artist || '未知艺术家',
    coverImage: props.coverImage
  };

  if (isCurrentTrack.value) {
    globalPlayer.togglePlay();
  } else {
    globalPlayer.playTrack(track);
  }
};

const addToPlaylist = () => {
  if (!globalPlayer) return;

  const track = {
    id: props.trackId || `track-${Date.now()}`,
    url: props.url,
    title: props.title || displayFileName.value,
    artist: props.artist || '未知艺术家',
    coverImage: props.coverImage
  };

  if (isInPlaylist.value) {
    globalPlayer.removeFromPlaylist(track.id);
  } else {
    globalPlayer.addToPlaylist(track);
  }
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
    audioError.value = false;
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

const onAudioError = () => {
  console.error('音频加载失败:', props.url);
  audioError.value = true;
  playing.value = false;
  duration.value = 0;
  currentTime.value = 0;
};

const onVideoError = () => {
  console.error('视频加载失败:', props.url);
};

const handleImageError = () => {
  imageError.value = true;
};

const formatTime = (seconds: number): string => {
  if (!isFinite(seconds) || seconds < 0) return '0:00';
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs.toString().padStart(2, '0')}`;
};

const updateMediaSession = () => {
  if (!('mediaSession' in navigator) || !isAudio.value || props.mode !== 'inline') return;

  try {
    navigator.mediaSession.metadata = new MediaMetadata({
      title: props.title || displayFileName.value,
      artist: props.artist || '未知艺术家',
      album: '',
      artwork: props.coverImage ? [
        {src: props.coverImage, sizes: '96x96', type: 'image/jpeg'},
        {src: props.coverImage, sizes: '128x128', type: 'image/jpeg'},
        {src: props.coverImage, sizes: '192x192', type: 'image/jpeg'},
        {src: props.coverImage, sizes: '256x256', type: 'image/jpeg'},
        {src: props.coverImage, sizes: '384x384', type: 'image/jpeg'},
        {src: props.coverImage, sizes: '512x512', type: 'image/jpeg'},
      ] : []
    });

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

// 仅在内联模式下监听 Media Session
watch(
    () => ({
      title: props.title,
      artist: props.artist,
      coverImage: props.coverImage,
      duration: duration.value
    }),
    () => {
      if (isAudio.value && duration.value > 0 && props.mode === 'inline') {
        updateMediaSession();
      }
    }
);

// 从全局播放器同步时长（global 模式）
watch(
    () => globalPlayer?.currentTrack.value,
    (track) => {
      if (props.mode === 'global' && track?.id === props.trackId) {
        duration.value = globalPlayer?.duration.value || 0;
      }
    }
);

onBeforeUnmount(() => {
  if ('mediaSession' in navigator && props.mode === 'inline') {
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