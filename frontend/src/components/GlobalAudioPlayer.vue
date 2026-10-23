<template>
  <!-- 全局浮动播放器 -->
  <Teleport to="body">
    <!-- 最小化悬浮胶囊按钮 -->
    <Transition name="fade-scale">
      <button
          v-if="currentTrack && isMinimized"
          @click="isMinimized = false"
          class="fixed bottom-6 right-6 z-50 flex h-14 w-14 items-center justify-center overflow-hidden rounded-full shadow-2xl transition-all duration-300 hover:scale-105 active:scale-95 group select-none"
          :aria-label="tr('common.expandPlayer')"
      >
        <!-- 封面底图 -->
        <div class="absolute inset-0">
          <img
              v-if="currentTrack.coverImage"
              :src="currentTrack.coverImage"
              :alt="currentTrack.title"
              class="h-full w-full object-cover"
          />
          <div
              v-else
              class="flex h-full w-full items-center justify-center bg-primary text-white"
          >
            <svg class="h-6 w-6" fill="currentColor" viewBox="0 0 20 20">
              <path d="M18 3a1 1 0 00-1.196-.98l-10 2A1 1 0 006 5v9.114A4.369 4.369 0 005 14c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V7.82l8-1.6v5.894A4.37 4.37 0 0015 12c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V3z"/>
            </svg>
          </div>
        </div>

        <!-- 半透明遮罩与播放图标 -->
        <div class="absolute inset-0 bg-black/40 flex items-center justify-center backdrop-blur-2xs">
          <svg v-if="!isPlaying" class="ml-0.5 h-6 w-6 text-white drop-shadow-md" fill="currentColor" viewBox="0 0 20 20">
            <path d="M6.3 2.841A1.5 1.5 0 004 4.11V15.89a1.5 1.5 0 002.3 1.269l9.344-5.89a1.5 1.5 0 000-2.538L6.3 2.84z"/>
          </svg>
          <svg v-else class="h-6 w-6 text-white drop-shadow-md" fill="currentColor" viewBox="0 0 20 20">
            <path d="M6 4h3v12H6V4zm5 0h3v12h-3V4z"/>
          </svg>
        </div>

        <!-- 播放进度环（修复数学表达式） -->
        <svg class="pointer-events-none absolute inset-0 h-14 w-14 -rotate-90" viewBox="0 0 64 64">
          <circle
              cx="32"
              cy="32"
              r="29"
              fill="none"
              stroke="rgba(255,255,255,0.25)"
              stroke-width="2.5"
          />
          <circle
              cx="32"
              cy="32"
              r="29"
              fill="none"
              stroke="#ffffff"
              stroke-width="2.5"
              stroke-dasharray="182.2"
              :stroke-dashoffset="182.2 - (182.2 * progressPercent / 100)"
              class="transition-all duration-100"
          />
        </svg>

        <!-- 悬停提示框 -->
        <div class="pointer-events-none absolute bottom-full mb-3 hidden opacity-0 transition-opacity group-hover:block group-hover:opacity-100">
          <div class="rounded-2xl bg-surface-container-high border border-outline/15 px-3 py-2 text-xs text-text shadow-xl whitespace-nowrap">
            <div class="font-bold text-text truncate max-w-[140px]">{{ currentTrack.title }}</div>
            <div class="text-[11px] text-text-secondary truncate max-w-[140px] mt-0.5">{{ currentTrack.artist }}</div>
          </div>
        </div>
      </button>
    </Transition>

    <!-- 完整播放器底栏：MD3 动态表面 -->
    <Transition name="slide-up">
      <div
          v-if="currentTrack && !isMinimized"
          class="fixed bottom-0 left-0 right-0 z-50 border-t border-outline/10 bg-surface/90 shadow-2xl backdrop-blur-xl"
      >
        <div class="mx-auto max-w-screen-xl px-4 py-3">
          <div class="flex items-center gap-4">
            <!-- 歌曲信息 -->
            <router-link :to="{ name: 'article', params: { id: currentTrack.id } }" class="flex min-w-0 flex-1 items-center gap-3">
              <div class="shrink-0">
                <div v-if="currentTrack.coverImage" class="h-12 w-12 overflow-hidden rounded-2xl shadow-xs">
                  <img :src="currentTrack.coverImage" :alt="currentTrack.title" class="h-full w-full object-cover" />
                </div>
                <div v-else class="flex h-12 w-12 items-center justify-center rounded-2xl bg-surface-container text-primary">
                  <svg class="h-6 w-6" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M18 3a1 1 0 00-1.196-.98l-10 2A1 1 0 006 5v9.114A4.369 4.369 0 005 14c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V7.82l8-1.6v5.894A4.37 4.37 0 0015 12c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V3z"/>
                  </svg>
                </div>
              </div>

              <div class="min-w-0 flex-1">
                <h4 class="truncate text-xs font-bold text-text sm:text-sm">
                  {{ currentTrack.title }}
                </h4>
                <p class="truncate text-[11px] text-text-secondary">
                  {{ currentTrack.artist }}
                </p>
              </div>
            </router-link>

            <!-- 播放控制按钮 -->
            <div class="flex items-center gap-2">
              <button
                  @click="playPrevious"
                  :disabled="!hasPrevious"
                  class="flex h-9 w-9 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text disabled:opacity-30 disabled:cursor-not-allowed"
                  :aria-label="tr('common.previousTrack')"
              >
                <svg class="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M8.445 14.832A1 1 0 0010 14v-2.798l5.445 3.63A1 1 0 0017 14V6a1 1 0 00-1.555-.832L10 8.798V6a1 1 0 00-1.555-.832l-6 4a1 1 0 000 1.664l6 4z"/>
                </svg>
              </button>

              <button
                  @click="togglePlay"
                  class="flex h-11 w-11 items-center justify-center rounded-full bg-primary text-white shadow-xs transition hover:brightness-105 active:scale-95"
                  :aria-label="tr('common.togglePlayback')"
              >
                <svg v-if="!isPlaying" class="ml-0.5 h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M6.3 2.841A1.5 1.5 0 004 4.11V15.89a1.5 1.5 0 002.3 1.269l9.344-5.89a1.5 1.5 0 000-2.538L6.3 2.84z"/>
                </svg>
                <svg v-else class="h-5 w-5" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M6 4h3v12H6V4zm5 0h3v12h-3V4z"/>
                </svg>
              </button>

              <button
                  @click="playNext"
                  :disabled="!hasNext"
                  class="flex h-9 w-9 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text disabled:opacity-30 disabled:cursor-not-allowed"
                  :aria-label="tr('common.nextTrack')"
              >
                <svg class="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M4.555 5.168A1 1 0 003 6v8a1 1 0 001.555.832L10 11.202V14a1 1 0 001.555.832l6-4a1 1 0 000-1.664l-6-4A1 1 0 0010 6v2.798l-5.445-3.63z"/>
                </svg>
              </button>
            </div>

            <!-- 桌面端进度条 -->
            <div class="hidden md:flex flex-1 items-center gap-3">
              <span class="text-xs text-text-secondary tabular-nums">
                {{ formatTime(currentTime) }}
              </span>
              <div class="group relative flex-1 cursor-pointer py-2" @click="seek">
                <div class="relative h-1.5 w-full rounded-full bg-surface-container overflow-hidden">
                  <div
                      class="h-full bg-primary rounded-full transition-all duration-100"
                      :style="{ width: progressPercent + '%' }"
                  ></div>
                </div>
              </div>
              <span class="text-xs text-text-secondary tabular-nums">
                {{ formatTime(duration) }}
              </span>
            </div>

            <!-- 操作按钮群 -->
            <div class="flex items-center gap-1">
              <button
                  @click="showPlaylist = !showPlaylist"
                  class="relative flex h-8 w-8 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text"
                  :class="{ 'bg-surface-container text-primary': showPlaylist }"
                  :aria-label="tr('common.playlist')"
              >
                <svg class="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M3 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h6a1 1 0 110 2H4a1 1 0 01-1-1z"/>
                </svg>
                <span v-if="playlist.length > 0" class="absolute -top-0.5 -right-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-primary text-[10px] font-bold text-white">
                  {{ playlist.length }}
                </span>
              </button>

              <button
                  @click="minimizePlayer"
                  class="flex h-8 w-8 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text"
                  :aria-label="tr('common.minimizePlayer')"
              >
                <svg class="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M3 10a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z" clip-rule="evenodd"/>
                </svg>
              </button>

              <button
                  @click="closePlayer"
                  class="flex h-8 w-8 items-center justify-center rounded-full text-text-secondary transition hover:bg-surface-container hover:text-text"
                  :aria-label="tr('common.closePlayer')"
              >
                <svg class="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
                </svg>
              </button>
            </div>
          </div>

          <!-- 移动端进度条 -->
          <div class="mt-2 flex items-center gap-2 md:hidden">
            <span class="text-[11px] text-text-secondary tabular-nums">
              {{ formatTime(currentTime) }}
            </span>
            <div class="relative flex-1 cursor-pointer py-1.5" @click="seek">
              <div class="relative h-1 w-full rounded-full bg-surface-container overflow-hidden">
                <div
                    class="h-full bg-primary rounded-full transition-all duration-100"
                    :style="{ width: progressPercent + '%' }"
                ></div>
              </div>
            </div>
            <span class="text-[11px] text-text-secondary tabular-nums">
              {{ formatTime(duration) }}
            </span>
          </div>
        </div>

        <!-- 播放列表弹出面板 -->
        <Transition name="slide-down">
          <div
              v-if="showPlaylist"
              class="max-h-80 overflow-y-auto border-t border-outline/10 bg-surface-container/30"
          >
            <div class="mx-auto max-w-screen-xl px-4 py-4">
              <div class="mb-3 flex items-center justify-between">
                <h3 class="text-xs font-bold text-text sm:text-sm">
                  {{ tr('common.playlistCount', { count: playlist.length }) }}
                </h3>
                <button
                    v-if="playlist.length > 0"
                    @click="clearPlaylist"
                    class="text-xs font-semibold text-red-600 dark:text-red-400 hover:underline"
                >
                  {{ tr('common.clearPlaylist') }}
                </button>
              </div>

              <div v-if="playlist.length === 0" class="py-8 text-center text-xs text-text-secondary">
                <p>{{ tr('common.emptyPlaylist') }}</p>
              </div>

              <div v-else class="space-y-1">
                <div
                    v-for="(track, index) in playlist"
                    :key="track.id"
                    @click="playTrack(track)"
                    class="group flex cursor-pointer items-center gap-3 rounded-2xl p-2 transition hover:bg-surface"
                    :class="{ 'bg-primary/10 shadow-2xs': currentTrack?.id === track.id }"
                >
                  <div class="w-6 shrink-0 text-center">
                    <span v-if="currentTrack?.id === track.id && isPlaying" class="text-primary font-bold">
                      ▶
                    </span>
                    <span v-else class="text-xs text-text-secondary">{{ index + 1 }}</span>
                  </div>

                  <div class="h-9 w-9 shrink-0 overflow-hidden rounded-xl bg-surface-container">
                    <img v-if="track.coverImage" :src="track.coverImage" :alt="track.title" class="h-full w-full object-cover"/>
                  </div>

                  <div class="min-w-0 flex-1">
                    <p class="truncate text-xs font-semibold text-text">{{ track.title }}</p>
                    <p class="truncate text-[11px] text-text-secondary">{{ track.artist }}</p>
                  </div>

                  <button
                      @click.stop="removeFromPlaylist(track.id)"
                      class="flex h-7 w-7 items-center justify-center rounded-full text-text-secondary opacity-0 transition hover:bg-red-500/10 hover:text-red-600 group-hover:opacity-100"
                      :aria-label="tr('common.remove')"
                  >
                    ✕
                  </button>
                </div>
              </div>
            </div>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>

  <!-- 隐藏的音频元素 -->
  <audio
      ref="audioRef"
      @timeupdate="onTimeUpdate"
      @loadedmetadata="onLoadedMetadata"
      @ended="onEnded"
      @play="onPlay"
      @pause="onPause"
      @error="onError"
      class="hidden"
  ></audio>
  <slot></slot>
</template>

<script setup lang="ts">
import { translate as tr } from '@/i18n';
import { ref, computed, provide, watch, onMounted, onBeforeUnmount } from 'vue';

interface AudioTrack {
  id: string;
  url: string;
  title: string;
  artist: string;
  coverImage?: string;
}

defineOptions({ name: 'GlobalAudioPlayer' });

const STORAGE_KEY = 'audio_player_state';
const MAX_PLAYLIST_SIZE = 50;
const SAVE_INTERVAL = 5000;

const audioRef = ref<HTMLAudioElement | null>(null);
const currentTrack = ref<AudioTrack | null>(null);
const playlist = ref<AudioTrack[]>([]);
const isPlaying = ref(false);
const currentTime = ref(0);
const duration = ref(0);
const showPlaylist = ref(false);
const isMinimized = ref(false);

let lastSaveTime = 0;

const loadState = () => {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      const state = JSON.parse(saved);
      if (state.playlist && Array.isArray(state.playlist)) {
        playlist.value = state.playlist;
      }
      if (state.currentTrack && audioRef.value) {
        currentTrack.value = state.currentTrack;
        audioRef.value.src = state.currentTrack.url;
        if (state.currentTime) {
          const handleMetadata = () => {
            if (audioRef.value && state.currentTime) {
              audioRef.value.currentTime = state.currentTime;
            }
          };
          audioRef.value.addEventListener('loadedmetadata', handleMetadata, { once: true });
        }
      }
      if (state.isMinimized !== undefined) {
        isMinimized.value = state.isMinimized;
      }
    }
  } catch (error) {
    console.error('加载播放器状态失败:', error);
    localStorage.removeItem(STORAGE_KEY);
  }
};

const saveState = () => {
  try {
    const state = {
      playlist: playlist.value.slice(0, MAX_PLAYLIST_SIZE),
      currentTrack: currentTrack.value,
      currentTime: currentTime.value,
      isMinimized: isMinimized.value,
      timestamp: Date.now()
    };
    const stateStr = JSON.stringify(state);
    // 修复了减号与乘号表达式
    if (stateStr.length > 2 * 1024 * 1024) {
      state.playlist = playlist.value.slice(0, 20);
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    lastSaveTime = Date.now();
  } catch (error) {
    console.error('保存播放器状态失败:', error);
  }
};

const throttledSave = () => {
  const now = Date.now();
  // 修复了减号表达式
  if (now - lastSaveTime >= SAVE_INTERVAL) {
    saveState();
  }
};

onMounted(() => {
  loadState();
});

onBeforeUnmount(() => {
  saveState();
});

watch(currentTime, () => {
  if (currentTrack.value) throttledSave();
});

watch(isMinimized, () => {
  saveState();
});

const progressPercent = computed(() =>
    duration.value > 0 ? (currentTime.value / duration.value) * 100 : 0
);

const currentTrackIndex = computed(() => {
  if (!currentTrack.value) return -1;
  return playlist.value.findIndex(t => t.id === currentTrack.value!.id);
});

const hasPrevious = computed(() => currentTrackIndex.value > 0);
const hasNext = computed(() =>
    currentTrackIndex.value >= 0 && currentTrackIndex.value < playlist.value.length - 1
);

const minimizePlayer = () => {
  isMinimized.value = true;
  showPlaylist.value = false;
};

const playTrack = (track: AudioTrack) => {
  if (!audioRef.value) return;
  currentTrack.value = track;
  audioRef.value.src = track.url;
  audioRef.value.play();
  if (!playlist.value.some(t => t.id === track.id)) {
    playlist.value.push(track);
  }
  saveState();
};

const togglePlay = () => {
  if (!audioRef.value || !currentTrack.value) return;
  if (isPlaying.value) {
    audioRef.value.pause();
  } else {
    audioRef.value.play();
  }
};

const playNext = () => {
  if (!hasNext.value) return;
  const nextTrack = playlist.value[currentTrackIndex.value + 1];
  if (nextTrack) playTrack(nextTrack);
};

const playPrevious = () => {
  if (!hasPrevious.value) return;
  const prevTrack = playlist.value[currentTrackIndex.value - 1];
  if (prevTrack) playTrack(prevTrack);
};

const seek = (event: MouseEvent) => {
  if (!audioRef.value || duration.value <= 0) return;
  const progressBar = event.currentTarget as HTMLElement;
  const rect = progressBar.getBoundingClientRect();
  const clickX = event.clientX - rect.left;
  const percent = clickX / rect.width;
  audioRef.value.currentTime = percent * duration.value;
  saveState();
};

const addToPlaylist = (track: AudioTrack) => {
  if (!playlist.value.some(t => t.id === track.id)) {
    playlist.value.push(track);
    saveState();
  }
};

const removeFromPlaylist = (trackId: string) => {
  playlist.value = playlist.value.filter(t => t.id !== trackId);
  if (currentTrack.value?.id === trackId) {
    if (playlist.value.length > 0) {
      playTrack(playlist.value[0]);
    } else {
      closePlayer();
    }
  }
  saveState();
};

const clearPlaylist = () => {
  playlist.value = [];
  closePlayer();
  saveState();
};

const closePlayer = () => {
  if (audioRef.value) {
    audioRef.value.pause();
    audioRef.value.src = '';
  }
  currentTrack.value = null;
  isPlaying.value = false;
  showPlaylist.value = false;
  isMinimized.value = false;
  saveState();
};

const onTimeUpdate = () => {
  if (audioRef.value) currentTime.value = audioRef.value.currentTime;
};

const onLoadedMetadata = () => {
  if (audioRef.value) {
    duration.value = audioRef.value.duration;
  }
};

const onEnded = () => {
  isPlaying.value = false;
  if (hasNext.value) playNext();
};

const onPlay = () => { isPlaying.value = true; };
const onPause = () => { isPlaying.value = false; };
const onError = () => { if (hasNext.value) playNext(); };

const formatTime = (seconds: number): string => {
  if (!isFinite(seconds) || seconds < 0) return '0:00';
  const mins = Math.floor(seconds / 60);
  const secs = Math.floor(seconds % 60);
  return `${mins}:${secs.toString().padStart(2, '0')}`;
};

provide('globalAudioPlayer', {
  currentTrack,
  currentTrackId: computed(() => currentTrack.value?.id || null),
  isPlaying,
  duration,
  playlist,
  playTrack,
  togglePlay,
  addToPlaylist,
  removeFromPlaylist
});

watch(currentTrack, (track) => {
  if (!track || !('mediaSession' in navigator)) return;
  try {
    navigator.mediaSession.metadata = new MediaMetadata({
      title: track.title,
      artist: track.artist,
      artwork: track.coverImage ? [
        { src: track.coverImage, sizes: '96x96', type: 'image/jpeg' },
        { src: track.coverImage, sizes: '512x512', type: 'image/jpeg' },
      ] : []
    });
    navigator.mediaSession.setActionHandler('play', togglePlay);
    navigator.mediaSession.setActionHandler('pause', togglePlay);
    navigator.mediaSession.setActionHandler('previoustrack', playPrevious);
    navigator.mediaSession.setActionHandler('nexttrack', playNext);
  } catch (err) {
    console.warn('Media Session API not supported', err);
  }
});
</script>

<style scoped>
.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.3s cubic-bezier(0.2, 0, 0, 1);
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: max-height 0.3s ease-out, opacity 0.2s ease-out;
}

.slide-down-enter-from,
.slide-down-leave-to {
  max-height: 0;
  opacity: 0;
}

.fade-scale-enter-active,
.fade-scale-leave-active {
  transition: all 0.25s cubic-bezier(0.2, 0, 0, 1);
}

.fade-scale-enter-from,
.fade-scale-leave-to {
  opacity: 0;
  transform: scale(0.85);
}
</style>
