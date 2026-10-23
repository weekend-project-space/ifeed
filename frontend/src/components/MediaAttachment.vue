<template>
  <section v-if="url" class="text-text">
    <!-- 支持的媒体类型 -->
    <div v-if="isSupported" class="overflow-hidden rounded-2xl bg-surface-container/40 transition duration-200 hover:bg-surface-container/70 shadow-2xs">

      <!-- 1. 视频播放器 -->
      <template v-if="isVideo">
        <div class="aspect-video bg-black">
          <video
              :src="url"
              controls
              controlsList="nodownload"
              class="h-full w-full"
              @error="onVideoError"
          ></video>
        </div>
        <div class="border-t border-outline/10 px-4 py-3">
          <div class="flex items-center gap-2.5">
            <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
              <svg class="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
                <path d="M2 6a2 2 0 012-2h6a2 2 0 012 2v8a2 2 0 01-2 2H4a2 2 0 01-2-2V6zm12.553 1.106A1 1 0 0014 8v4a1 1 0 00.553.894l2 1A1 1 0 0018 13V7a1 1 0 00-1.447-.894l-2 1z"/>
              </svg>
            </div>
            <div class="min-w-0 flex-1">
              <p class="truncate text-xs font-medium text-text">{{ displayFileName }}</p>
            </div>
          </div>
        </div>
      </template>

      <!-- 2. 音频播放器 -->
      <template v-else-if="isAudio">
        <!-- 内联模式 -->
        <div v-if="mode === 'inline'" class="p-4 sm:p-5">
          <div class="flex items-start gap-4">
            <!-- 封面图 + 播放按钮 -->
            <div class="group relative shrink-0">
              <div v-if="coverImage" class="h-20 w-20 overflow-hidden rounded-2xl shadow-xs sm:h-24 sm:w-24">
                <img
                    :src="coverImage"
                    :alt="title || tr('common.cover')"
                    class="h-full w-full object-cover"
                    @error="handleImageError"
                />
              </div>
              <div v-else class="flex h-20 w-20 items-center justify-center rounded-2xl bg-surface-container-high text-primary sm:h-24 sm:w-24">
                <svg class="h-8 w-8" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M18 3a1 1 0 00-1.196-.98l-10 2A1 1 0 006 5v9.114A4.369 4.369 0 005 14c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V7.82l8-1.6v5.894A4.37 4.37 0 0015 12c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V3z"/>
                </svg>
              </div>

              <!-- 覆盖按钮 -->
              <button
                  @click="toggleAudio"
                  class="absolute inset-0 flex items-center justify-center rounded-2xl bg-black/35 opacity-0 transition duration-200 group-hover:opacity-100 focus:opacity-100"
                  :class="{ 'opacity-100': isPlaying }"
                  :aria-label="tr('common.togglePlayback')"
              >
                <div class="flex h-11 w-11 items-center justify-center rounded-full bg-surface text-text shadow-xs transition hover:scale-105 active:scale-95">
                  <svg v-if="!isPlaying" class="ml-0.5 h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M6.3 2.841A1.5 1.5 0 004 4.11V15.89a1.5 1.5 0 002.3 1.269l9.344-5.89a1.5 1.5 0 000-2.538L6.3 2.84z"/>
                  </svg>
                  <svg v-else class="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M5 4a2 2 0 012-2h1a2 2 0 012 2v12a2 2 0 01-2 2H7a2 2 0 01-2-2V4zm8 0a2 2 0 012-2h1a2 2 0 012 2v12a2 2 0 01-2 2h-1a2 2 0 01-2-2V4z"/>
                  </svg>
                </div>
              </button>
            </div>

            <!-- 控制与进度 -->
            <div class="min-w-0 flex-1">
              <div class="mb-1.5">
                <h3 v-if="title" class="truncate text-sm font-semibold text-text">
                  {{ title }}
                </h3>
                <p v-if="artist" class="truncate text-xs text-text-secondary">
                  {{ artist }}
                </p>
                <p v-else class="truncate text-xs text-text-secondary">
                  {{ displayFileName }}
                </p>
              </div>

              <div class="mb-1 text-[11px] text-text-secondary tabular-nums" v-if="duration > 0">
                {{ formatTime(currentTime) }} / {{ formatTime(duration) }}
              </div>

              <!-- 进度条 -->
              <div v-if="duration > 0" class="group relative cursor-pointer py-2" @click="seekAudio">
                <div class="relative h-1.5 w-full rounded-full bg-surface-container-high overflow-hidden">
                  <div class="h-full bg-primary rounded-full transition-all duration-100" :style="{ width: progressPercent + '%' }"></div>
                </div>
              </div>

              <div v-if="audioError" class="mt-1 text-xs text-red-600 dark:text-red-400">
                {{ tr('common.audioLoadFailed') }}
              </div>
            </div>
          </div>
        </div>

        <!-- 全局播放器触发卡片模式 -->
        <div v-else class="p-4">
          <div class="flex items-center gap-3.5">
            <!-- 缩略图 -->
            <div class="shrink-0">
              <div v-if="coverImage" class="h-14 w-14 overflow-hidden rounded-xl shadow-2xs">
                <img :src="coverImage" :alt="title || tr('common.cover')" class="h-full w-full object-cover" @error="handleImageError" />
              </div>
              <div v-else class="flex h-14 w-14 items-center justify-center rounded-xl bg-surface-container text-primary">
                <svg class="h-6 w-6" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M18 3a1 1 0 00-1.196-.98l-10 2A1 1 0 006 5v9.114A4.369 4.369 0 005 14c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V7.82l8-1.6v5.894A4.37 4.37 0 0015 12c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V3z"/>
                </svg>
              </div>
            </div>

            <!-- 信息 -->
            <div class="min-w-0 flex-1">
              <h3 v-if="title" class="truncate text-xs font-semibold text-text sm:text-sm">
                {{ title }}
              </h3>
              <p class="truncate text-[11px] text-text-secondary">
                {{ artist || displayFileName }}
              </p>
              <p v-if="duration > 0" class="mt-0.5 text-[10px] text-text-secondary/70 tabular-nums">
                {{ formatTime(duration) }}
              </p>
            </div>

            <!-- 操作按钮 -->
            <div class="flex items-center gap-2">
              <button
                  @click="playInGlobalPlayer"
                  class="flex h-9 w-9 items-center justify-center rounded-full bg-primary text-white shadow-xs transition hover:brightness-105 active:scale-95"
                  :aria-label="tr('common.play')"
              >
                <svg v-if="!isCurrentTrack || !isPlaying" class="ml-0.5 h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M6.3 2.841A1.5 1.5 0 004 4.11V15.89a1.5 1.5 0 002.3 1.269l9.344-5.89a1.5 1.5 0 000-2.538L6.3 2.84z"/>
                </svg>
                <svg v-else class="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M6 4h2.5v12H6V4zm5.5 0H14v12h-2.5V4z"/>
                </svg>
              </button>

              <button
                  @click="addToPlaylist"
                  class="flex h-9 w-9 items-center justify-center rounded-full bg-surface text-text-secondary shadow-2xs transition hover:bg-surface-container active:scale-95"
                  :class="{ 'text-primary font-bold': isInPlaylist }"
                  :aria-label="tr('common.addToPlaylist')"
              >
                <svg v-if="!isInPlaylist" class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                </svg>
                <svg v-else class="h-4 w-4 text-primary" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                </svg>
              </button>
            </div>
          </div>
        </div>

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

      <!-- 3. 图片展示 -->
      <template v-else-if="isImage">
        <div class="bg-surface-container/20">
          <img
              :src="url"
              :alt="displayFileName"
              class="max-h-[500px] w-full object-contain"
              loading="lazy"
          />
        </div>
        <div class="border-t border-outline/10 px-4 py-3">
          <p class="truncate text-xs font-medium text-text-secondary">{{ displayFileName }}</p>
        </div>
      </template>
    </div>

    <!-- 不支持的附件类型 -->
    <a
        v-else
        :href="url"
        target="_blank"
        rel="noopener noreferrer"
        class="inline-flex items-center gap-2 rounded-full bg-surface-container/60 px-4 py-2 text-xs font-medium text-text transition hover:bg-surface-container active:scale-95"
    >
      <svg class="h-4 w-4 text-text-secondary" fill="currentColor" viewBox="0 0 20 20">
        <path fill-rule="evenodd"
              d="M8 4a3 3 0 00-3 3v4a5 5 0 0010 0V7a1 1 0 112 0v4a7 7 0 11-14 0V7a5 5 0 0110 0v4a3 3 0 11-6 0V7a1 1 0 012 0v4a1 1 0 102 0V7a3 3 0 00-3-3z"
              clip-rule="evenodd"/>
      </svg>
      <span>{{ tr('common.viewAttachment') }}</span>
    </a>
  </section>
</template>

<script setup lang="ts">
import { translate as tr } from '@/i18n';
import { computed, ref, watch, onBeforeUnmount, inject } from 'vue';

interface Props {
  url: string | null;
  type?: string | null;
  title?: string | null;
  artist?: string | null;
  coverImage?: string | null;
  mode?: 'inline' | 'global';
  trackId?: string | null;
}

const props = withDefaults(defineProps<Props>(), {
  type: null,
  title: null,
  artist: null,
  coverImage: null,
  mode: 'inline',
  trackId: null
});

const globalPlayer = inject<any>('globalAudioPlayer', null);

const audioRef = ref<HTMLAudioElement | null>(null);
const playing = ref(false);
const currentTime = ref(0);
const duration = ref(0);
const imageError = ref(false);
const audioError = ref(false);

const isCurrentTrack = computed(() => {
  if (!globalPlayer || !props.trackId) return false;
  return globalPlayer.currentTrackId.value === props.trackId;
});

const isPlaying = computed(() => {
  if (props.mode === 'inline') return playing.value;
  return isCurrentTrack.value && globalPlayer?.isPlaying.value;
});

const isInPlaylist = computed(() => {
  if (!globalPlayer || !props.trackId) return false;
  return globalPlayer.playlist.value.some((track: any) => track.id === props.trackId);
});

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
    props.url?.split('/').pop()?.split('?')[0] || tr('common.unknownFile')
);

const progressPercent = computed(() =>
    duration.value > 0 ? (currentTime.value / duration.value) * 100 : 0
);

const coverImage = computed(() =>
    imageError.value ? null : props.coverImage
);

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

const playInGlobalPlayer = () => {
  if (!globalPlayer) return;
  const track = {
    id: props.trackId || `track-${Date.now()}`,
    url: props.url,
    title: props.title || displayFileName.value,
    artist: props.artist || tr('common.unknownArtist'),
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
    artist: props.artist || tr('common.unknownArtist'),
    coverImage: props.coverImage
  };
  if (isInPlaylist.value) {
    globalPlayer.removeFromPlaylist(track.id);
  } else {
    globalPlayer.addToPlaylist(track);
  }
};

const onTimeUpdate = () => {
  if (audioRef.value) currentTime.value = audioRef.value.currentTime;
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

const onPlay = () => { playing.value = true; };
const onPause = () => { playing.value = false; };

const onAudioError = () => {
  console.error('音频加载失败:', props.url);
  audioError.value = true;
  playing.value = false;
  duration.value = 0;
  currentTime.value = 0;
};

const onVideoError = () => { console.error('视频加载失败:', props.url); };
const handleImageError = () => { imageError.value = true; };

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
      artist: props.artist || tr('common.unknownArtist'),
      album: '',
      artwork: props.coverImage ? [
        { src: props.coverImage, sizes: '96x96', type: 'image/jpeg' },
        { src: props.coverImage, sizes: '128x128', type: 'image/jpeg' },
        { src: props.coverImage, sizes: '256x256', type: 'image/jpeg' },
      ] : []
    });

    navigator.mediaSession.setActionHandler('play', () => { audioRef.value?.play(); });
    navigator.mediaSession.setActionHandler('pause', () => { audioRef.value?.pause(); });
    navigator.mediaSession.setActionHandler('seekbackward', () => {
      if (audioRef.value) audioRef.value.currentTime = Math.max(0, audioRef.value.currentTime - 10);
    });
    navigator.mediaSession.setActionHandler('seekforward', () => {
      if (audioRef.value && duration.value > 0) audioRef.value.currentTime = Math.min(duration.value, audioRef.value.currentTime + 10);
    });
    navigator.mediaSession.setActionHandler('seekto', (details) => {
      if (audioRef.value && details.seekTime != null) audioRef.value.currentTime = details.seekTime;
    });
  } catch (err) {
    console.warn('Media Session API not fully supported', err);
  }
};

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