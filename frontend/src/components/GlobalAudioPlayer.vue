<!-- components/GlobalAudioPlayer.vue -->
<template>
  <!-- 全局浮动播放器 -->
  <Teleport to="body">
    <!-- 最小化按钮 -->
    <Transition name="fade-scale">
      <button
          v-if="currentTrack && isMinimized"
          @click="isMinimized = false"
          class="fixed bottom-6 right-6 w-16 h-16 rounded-full shadow-2xl transition-all hover:scale-110 z-50 group overflow-hidden"
          aria-label="展开播放器"
      >
        <!-- 封面背景 -->
        <div class="absolute inset-0">
          <img
              v-if="currentTrack.coverImage"
              :src="currentTrack.coverImage"
              :alt="currentTrack.title"
              class="w-full h-full object-cover"
          />
          <div
              v-else
              class="w-full h-full bg-gradient-to-br from-primary to-primary/80 flex items-center justify-center"
          >
            <svg class="w-8 h-8 text-white" fill="currentColor" viewBox="0 0 20 20">
              <path d="M18 3a1 1 0 00-1.196-.98l-10 2A1 1 0 006 5v9.114A4.369 4.369 0 005 14c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V7.82l8-1.6v5.894A4.37 4.37 0 0015 12c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V3z"/>
            </svg>
          </div>
        </div>

        <!-- 半透明遮罩 -->
        <div class="absolute inset-0 bg-black/30"></div>

        <!-- 播放/暂停图标 -->
        <div class="absolute inset-0 flex items-center justify-center">
          <svg v-if="!isPlaying" class="w-6 h-6 text-white drop-shadow-lg ml-0.5" fill="currentColor" viewBox="0 0 20 20">
            <path d="M6.3 2.841A1.5 1.5 0 004 4.11V15.89a1.5 1.5 0 002.3 1.269l9.344-5.89a1.5 1.5 0 000-2.538L6.3 2.84z"/>
          </svg>
          <svg v-else class="w-6 h-6 text-white drop-shadow-lg" fill="currentColor" viewBox="0 0 20 20">
            <path d="M6 4h3v12H6V4zm5 0h3v12h-3V4z"/>
          </svg>
        </div>

        <!-- 播放进度环 -->
        <svg class="absolute inset-0 w-16 h-16 -rotate-90 pointer-events-none" viewBox="0 0 64 64">
          <circle
              cx="32"
              cy="32"
              r="30"
              fill="none"
              stroke="rgba(255,255,255,0.3)"
              stroke-width="2"
          />
          <circle
              cx="32"
              cy="32"
              r="30"
              fill="none"
              stroke="white"
              stroke-width="2"
              stroke-dasharray="188.4"
              :stroke-dashoffset="188.4 - (188.4 * progressPercent / 100)"
              class="transition-all duration-100 drop-shadow-lg"
          />
        </svg>

        <!-- 悬停提示 -->
        <div class="absolute bottom-full mb-3 opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none">
          <div class="bg-gray-900 text-white text-xs px-3 py-2 rounded-lg shadow-lg whitespace-nowrap">
            <div class="font-medium">{{ currentTrack.title }}</div>
            <div class="text-gray-300 mt-0.5">{{ currentTrack.artist }}</div>
          </div>
          <div class="w-2 h-2 bg-gray-900 transform rotate-45 mx-auto -mt-1"></div>
        </div>
      </button>
    </Transition>

    <!-- 完整播放器 -->
    <Transition name="slide-up">
      <div
          v-if="currentTrack && !isMinimized"
          class="fixed bottom-0 left-0 right-0 bg-white dark:bg-gray-900 border-t border-gray-200 dark:border-gray-700 shadow-2xl z-50"
      >
        <!-- 主播放控制区 -->
        <div class="max-w-screen-xl mx-auto px-4 py-3">
          <div class="flex items-center gap-4">
            <!-- 当前播放音乐信息 -->
            <router-link :to="{ name: 'article', params: { id: currentTrack.id } }" class="flex items-center gap-3 flex-1 min-w-0">
              <!-- 封面 -->
              <div class="flex-shrink-0">
                <div
                    v-if="currentTrack.coverImage"
                    class="w-14 h-14 rounded-lg overflow-hidden"
                >
                  <img
                      :src="currentTrack.coverImage"
                      :alt="currentTrack.title"
                      class="w-full h-full object-cover"
                  />
                </div>
                <div
                    v-else
                    class="w-14 h-14 rounded-lg bg-primary/10 dark:bg-primary/20 flex items-center justify-center"
                >
                  <svg class="w-7 h-7 text-primary" fill="currentColor" viewBox="0 0 20 20">
                    <path
                        d="M18 3a1 1 0 00-1.196-.98l-10 2A1 1 0 006 5v9.114A4.369 4.369 0 005 14c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V7.82l8-1.6v5.894A4.37 4.37 0 0015 12c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V3z"/>
                  </svg>
                </div>
              </div>

              <!-- 歌曲信息 -->
              <div class="flex-1 min-w-0">
                <h4 class="text-sm font-medium text-gray-900 dark:text-gray-100 truncate">
                  {{ currentTrack.title }}
                </h4>
                <p class="text-xs text-gray-600 dark:text-gray-400 truncate">
                  {{ currentTrack.artist }}
                </p>
              </div>
            </router-link>

            <!-- 播放控制按钮 -->
            <div class="flex items-center gap-2">
              <!-- 上一首 -->
              <button
                  @click="playPrevious"
                  :disabled="!hasPrevious"
                  class="w-9 h-9 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 flex items-center justify-center transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
                  aria-label="上一首"
              >
                <svg class="w-5 h-5 text-gray-700 dark:text-gray-300" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M8.445 14.832A1 1 0 0010 14v-2.798l5.445 3.63A1 1 0 0017 14V6a1 1 0 00-1.555-.832L10 8.798V6a1 1 0 00-1.555-.832l-6 4a1 1 0 000 1.664l6 4z"/>
                </svg>
              </button>

              <!-- 播放/暂停 -->
              <button
                  @click="togglePlay"
                  class="w-11 h-11 rounded-full bg-primary hover:bg-primary/90 flex items-center justify-center transition-colors focus:outline-none focus:ring-2 focus:ring-primary/50"
                  aria-label="播放/暂停"
              >
                <svg v-if="!isPlaying" class="w-6 h-6 text-white ml-0.5" fill="currentColor" viewBox="0 0 20 20">
                  <path
                      d="M6.3 2.841A1.5 1.5 0 004 4.11V15.89a1.5 1.5 0 002.3 1.269l9.344-5.89a1.5 1.5 0 000-2.538L6.3 2.84z"/>
                </svg>
                <svg v-else class="w-6 h-6 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M6 4h3v12H6V4zm5 0h3v12h-3V4z"/>
                </svg>
              </button>

              <!-- 下一首 -->
              <button
                  @click="playNext"
                  :disabled="!hasNext"
                  class="w-9 h-9 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 flex items-center justify-center transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
                  aria-label="下一首"
              >
                <svg class="w-5 h-5 text-gray-700 dark:text-gray-300" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M4.555 5.168A1 1 0 003 6v8a1 1 0 001.555.832L10 11.202V14a1 1 0 001.555.832l6-4a1 1 0 000-1.664l-6-4A1 1 0 0010 6v2.798l-5.445-3.63z"/>
                </svg>
              </button>
            </div>

            <!-- 进度条和时间 -->
            <div class="hidden md:flex items-center gap-3 flex-1">
              <span class="text-xs text-gray-600 dark:text-gray-400 tabular-nums">
                {{ formatTime(currentTime) }}
              </span>
              <div class="relative flex-1 py-2 cursor-pointer group" @click="seek">
                <div class="relative w-full h-1 bg-gray-300 dark:bg-gray-600 rounded-full">
                  <div
                      class="absolute inset-y-0 left-0 bg-primary rounded-full transition-all duration-100"
                      :style="{ width: progressPercent + '%' }"
                  ></div>
                </div>
                <div
                    class="absolute top-1/2 -translate-y-1/2 w-3 h-3 bg-white dark:bg-gray-200 border-2 border-primary rounded-full shadow-md transition-opacity pointer-events-none opacity-0 group-hover:opacity-100"
                    :style="{ left: `calc(${progressPercent}% - 6px)` }"
                ></div>
              </div>
              <span class="text-xs text-gray-600 dark:text-gray-400 tabular-nums">
                {{ formatTime(duration) }}
              </span>
            </div>

            <!-- 播放列表、最小化和关闭按钮 -->
            <div class="flex items-center gap-2">
              <!-- 播放列表按钮 -->
              <button
                  @click="showPlaylist = !showPlaylist"
                  class="w-9 h-9 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 flex items-center justify-center transition-colors relative"
                  :class="{ 'bg-gray-100 dark:bg-gray-800': showPlaylist }"
                  aria-label="播放列表"
              >
                <svg class="w-5 h-5 text-gray-700 dark:text-gray-300" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M3 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h6a1 1 0 110 2H4a1 1 0 01-1-1z"/>
                </svg>
                <span v-if="playlist.length > 0" class="absolute -top-1 -right-1 w-5 h-5 bg-primary text-white text-xs rounded-full flex items-center justify-center">
                  {{ playlist.length }}
                </span>
              </button>

              <!-- 最小化按钮 -->
              <button
                  @click="minimizePlayer"
                  class="w-9 h-9 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 flex items-center justify-center transition-colors"
                  aria-label="最小化播放器"
              >
                <svg class="w-5 h-5 text-gray-700 dark:text-gray-300" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M3 10a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z" clip-rule="evenodd"/>
                </svg>
              </button>

              <!-- 关闭按钮 -->
              <button
                  @click="closePlayer"
                  class="w-9 h-9 rounded-full hover:bg-gray-100 dark:hover:bg-gray-800 flex items-center justify-center transition-colors"
                  aria-label="关闭播放器"
              >
                <svg class="w-5 h-5 text-gray-700 dark:text-gray-300" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
                </svg>
              </button>
            </div>
          </div>

          <!-- 移动端进度条 -->
          <div class="md:hidden mt-2 flex items-center gap-2">
            <span class="text-xs text-gray-600 dark:text-gray-400 tabular-nums">
              {{ formatTime(currentTime) }}
            </span>
            <div class="relative flex-1 py-2 cursor-pointer group" @click="seek">
              <div class="relative w-full h-1 bg-gray-300 dark:bg-gray-600 rounded-full">
                <div
                    class="absolute inset-y-0 left-0 bg-primary rounded-full transition-all duration-100"
                    :style="{ width: progressPercent + '%' }"
                ></div>
              </div>
            </div>
            <span class="text-xs text-gray-600 dark:text-gray-400 tabular-nums">
              {{ formatTime(duration) }}
            </span>
          </div>
        </div>

        <!-- 播放列表面板 -->
        <Transition name="slide-down">
          <div
              v-if="showPlaylist"
              class="border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50 max-h-80 overflow-y-auto"
          >
            <div class="max-w-7xl mx-auto px-4 py-3">
              <div class="flex items-center justify-between mb-3">
                <h3 class="text-sm font-semibold text-gray-900 dark:text-gray-100">
                  播放列表 ({{ playlist.length }})
                </h3>
                <button
                    v-if="playlist.length > 0"
                    @click="clearPlaylist"
                    class="text-xs text-red-600 dark:text-red-400 hover:underline"
                >
                  清空列表
                </button>
              </div>

              <div v-if="playlist.length === 0" class="text-center py-8 text-gray-500 dark:text-gray-400">
                <svg class="w-12 h-12 mx-auto mb-2 opacity-50" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M18 3a1 1 0 00-1.196-.98l-10 2A1 1 0 006 5v9.114A4.369 4.369 0 005 14c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V7.82l8-1.6v5.894A4.37 4.37 0 0015 12c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V3z"/>
                </svg>
                <p class="text-sm">播放列表为空</p>
              </div>

              <div v-else class="space-y-1">
                <div
                    v-for="(track, index) in playlist"
                    :key="track.id"
                    @click="playTrack(track)"
                    class="flex items-center gap-3 p-2 rounded-lg hover:bg-white dark:hover:bg-gray-700 cursor-pointer transition-colors group"
                    :class="{ 'bg-primary/5 dark:bg-primary/10': currentTrack?.id === track.id }"
                >
                  <!-- 序号/播放状态 -->
                  <div class="w-6 text-center flex-shrink-0">
                    <span v-if="currentTrack?.id === track.id && isPlaying" class="text-primary">
                      <svg class="w-4 h-4 inline-block" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z" clip-rule="evenodd"/>
                      </svg>
                    </span>
                    <span v-else class="text-xs text-gray-500 dark:text-gray-400">{{ index + 1 }}</span>
                  </div>

                  <!-- 封面缩略图 -->
                  <div class="w-10 h-10 rounded overflow-hidden flex-shrink-0 bg-gray-200 dark:bg-gray-700">
                    <img v-if="track.coverImage" :src="track.coverImage" :alt="track.title" class="w-full h-full object-cover"/>
                    <div v-else class="w-full h-full flex items-center justify-center">
                      <svg class="w-5 h-5 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
                        <path d="M18 3a1 1 0 00-1.196-.98l-10 2A1 1 0 006 5v9.114A4.369 4.369 0 005 14c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V7.82l8-1.6v5.894A4.37 4.37 0 0015 12c-1.657 0-3 .895-3 2s1.343 2 3 2 3-.895 3-2V3z"/>
                      </svg>
                    </div>
                  </div>

                  <!-- 歌曲信息 -->
                  <div class="flex-1 min-w-0">
                    <p class="text-sm text-gray-900 dark:text-gray-100 truncate">{{ track.title }}</p>
                    <p class="text-xs text-gray-600 dark:text-gray-400 truncate">{{ track.artist }}</p>
                  </div>

                  <!-- 删除按钮 -->
                  <button
                      @click.stop="removeFromPlaylist(track.id)"
                      class="w-8 h-8 rounded-full hover:bg-red-50 dark:hover:bg-red-900/20 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
                      aria-label="移除"
                  >
                    <svg class="w-4 h-4 text-red-600 dark:text-red-400" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
                    </svg>
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
import {ref, computed, provide, watch, onMounted, onBeforeUnmount} from 'vue';

interface AudioTrack {
  id: string;
  url: string;
  title: string;
  artist: string;
  coverImage?: string;
}

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

      // 恢复最小化状态
      if (state.isMinimized !== undefined) {
        isMinimized.value = state.isMinimized;
      }

      // console.log('播放器状态已恢复');
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

    if (stateStr.length > 2 * 1024 * 1024) {
      console.warn('播放列表过大,仅保存前20首');
      state.playlist = playlist.value.slice(0, 20);
    }

    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    lastSaveTime = Date.now();
  } catch (error) {
    if (error instanceof Error && error.name === 'QuotaExceededError') {
      console.error('存储空间不足');
      try {
        const minState = {
          playlist: currentTrack.value ? [currentTrack.value] : [],
          currentTrack: currentTrack.value,
          currentTime: currentTime.value,
          isMinimized: isMinimized.value
        };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(minState));
      } catch (e) {
        console.error('最小化保存也失败:', e);
      }
    } else {
      console.error('保存播放器状态失败:', error);
    }
  }
};

const throttledSave = () => {
  const now = Date.now();
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
  if (currentTrack.value) {
    throttledSave();
  }
});

// 保存最小化状态
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
  if (audioRef.value) {
    currentTime.value = audioRef.value.currentTime;
  }
};

const onLoadedMetadata = () => {
  if (audioRef.value) {
    duration.value = audioRef.value.duration;
  }
};

const onEnded = () => {
  isPlaying.value = false;
  if (hasNext.value) {
    playNext();
  }
};

const onPlay = () => {
  isPlaying.value = true;
};

const onPause = () => {
  isPlaying.value = false;
};

const onError = () => {
  console.error('音频播放失败');
  if (hasNext.value) {
    playNext();
  }
};

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
        {src: track.coverImage, sizes: '96x96', type: 'image/jpeg'},
        {src: track.coverImage, sizes: '512x512', type: 'image/jpeg'},
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
  transition: transform 0.3s ease-out;
}

.slide-up-enter-from {
  transform: translateY(100%);
}

.slide-up-leave-to {
  transform: translateY(100%);
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: max-height 0.3s ease-out, opacity 0.3s ease-out;
}

.slide-down-enter-from,
.slide-down-leave-to {
  max-height: 0;
  opacity: 0;
}

.fade-scale-enter-active,
.fade-scale-leave-active {
  transition: all 0.3s ease;
}

.fade-scale-enter-from,
.fade-scale-leave-to {
  opacity: 0;
  transform: scale(0.8);
}
</style>