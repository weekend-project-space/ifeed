<template>
  <div class="adsterra-native-wrapper" :class="{ 'is-hidden': isBlocked }">
    <!-- 广告骨架/展示容器：固定最小高度，防止页面累积布局偏移 (CLS) -->
    <div
        ref="adContainerRef"
        class="adsterra-native-box"
        :style="{ minHeight: isLoaded ? 'auto' : '130px' }"
    >
      <!-- 动态沙盒 iframe：彻底隔绝 SPA 全局污染，每次切文章均视为独立冷启动 -->
      <iframe
          v-if="shouldRender"
          ref="iframeRef"
          class="adsterra-sandbox-frame"
          scrolling="no"
          frameborder="0"
          @load="handleIframeLoad"
      ></iframe>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'

interface Props {
  isNsfw?: boolean
  delay?: number
  cooldownHours?: number
  popunderCooldownHours?: number
}

const props = withDefaults(defineProps<Props>(), {
  isNsfw: false,
  delay: 6000,
  cooldownHours: 12,
  popunderCooldownHours: 24
})

const adContainerRef = ref<HTMLElement | null>(null)
const iframeRef = ref<HTMLIFrameElement | null>(null)
const shouldRender = ref(false)
const isLoaded = ref(false)
const isBlocked = ref(false)

const SOCIAL_SCRIPT_ID = 'adsterra-social-bar-script'
const POPUNDER_SCRIPT_ID = 'adsterra-popunder-script'

let socialBarTimer: ReturnType<typeof setTimeout> | null = null
let observer: IntersectionObserver | null = null

// 1. 构建 Native Banner 沙盒纯静态 HTML
const getSandboxHtml = () => {
  return `
    <!DOCTYPE html>
    <html>
      <head>
        <meta charset="utf-8">
        <style>
          * { margin: 0; padding: 0; box-sizing: border-box; }
          body {
            background: transparent;
            overflow: hidden;
            display: flex;
            justify-content: center;
            align-items: center;
          }
          #container-8d65aff59419c72acacec5abc483e65d {
            width: 100%;
            display: flex;
            justify-content: center;
          }
        </style>
      </head>
      <body>
        <div id="container-8d65aff59419c72acacec5abc483e65d"></div>
        <script async="async" data-cfasync="false" src="https://pl31300176.profitableratecpmnetwork.com/8d65aff59419c72acacec5abc483e65d/invoke.js"><\/script>
      </body>
    </html>
  `
}

// 写入 iframe 沙盒
const mountNativeAd = () => {
  shouldRender.value = true
  nextTick(() => {
    const iframe = iframeRef.value
    if (!iframe) return

    try {
      const doc = iframe.contentWindow?.document || iframe.contentDocument
      if (!doc) return

      doc.open()
      doc.write(getSandboxHtml())
      doc.close()
    } catch (err) {
      console.warn('Native Banner 沙盒注入异常:', err)
      isBlocked.value = true
    }
  })
}

// 动态撑开高度
const handleIframeLoad = () => {
  isLoaded.value = true
  const iframe = iframeRef.value
  if (!iframe) return

  setTimeout(() => {
    try {
      const contentHeight = iframe.contentWindow?.document?.body?.scrollHeight
      if (contentHeight && contentHeight > 20) {
        iframe.style.height = `${contentHeight}px`
      } else {
        iframe.style.height = '140px'
      }
    } catch {
      iframe.style.height = '140px'
    }
  }, 1000)
}

// 2. 挂载 Social Bar（必须严格满足 isNsfw）
const triggerSocialBar = () => {
  // 防线 1：非 NSFW 直接熔断退出
  if (!props.isNsfw) return

  const storageKey = 'ifeed_socialbar_last_shown'
  const lastShown = localStorage.getItem(storageKey)
  const cooldownMs = props.cooldownHours * 3600 * 1000

  if (lastShown && Date.now() - Number(lastShown) < cooldownMs) return

  // 清除未完成的旧定时器，防止切文章时的竞争条件
  if (socialBarTimer) {
    clearTimeout(socialBarTimer)
    socialBarTimer = null
  }

  socialBarTimer = setTimeout(() => {
    // 防线 2：延迟到达后二次核对，防止在这几秒内用户已经切到了非 NSFW 文章
    if (!props.isNsfw || document.getElementById(SOCIAL_SCRIPT_ID)) return

    const script = document.createElement('script')
    script.id = SOCIAL_SCRIPT_ID
    script.type = 'text/javascript'
    script.src = 'https://pl31301584.profitableratecpmnetwork.com/c0/e2/63/c0e263a132a477866eadb70aa49745e1.js'
    script.async = true
    document.body.appendChild(script)

    localStorage.setItem(storageKey, Date.now().toString())
  }, props.delay)
}

// 3. 挂载 Popunder（必须严格满足 isNsfw）
const triggerPopunder = () => {
  // 防线 1：修复原本漏掉的 isNsfw 判断，杜绝在普通文章上裸奔
  if (!props.isNsfw) return

  const storageKey = 'ifeed_popunder_last_shown'
  const lastShown = localStorage.getItem(storageKey)
  const cooldownMs = props.popunderCooldownHours * 3600 * 1000

  if (lastShown && Date.now() - Number(lastShown) < cooldownMs) return
  if (document.getElementById(POPUNDER_SCRIPT_ID)) return

  const script = document.createElement('script')
  script.id = POPUNDER_SCRIPT_ID
  script.type = 'text/javascript'
  script.src = 'https://pl31300178.profitableratecpmnetwork.com/bf/9e/06/bf9e0661b59ca3b97a16b584a16f0b7e.js'
  script.async = true
  document.body.appendChild(script)

  localStorage.setItem(storageKey, Date.now().toString())
}

// 彻底清除动态广告残留（防止污染后续正常文章）
const cleanupDynamicAds = () => {
  if (socialBarTimer) {
    clearTimeout(socialBarTimer)
    socialBarTimer = null
  }
  const sScript = document.getElementById(SOCIAL_SCRIPT_ID)
  if (sScript) sScript.remove()

  const pScript = document.getElementById(POPUNDER_SCRIPT_ID)
  if (pScript) pScript.remove()
}

// 调度器
const evaluateAds = (isAdult: boolean) => {
  if (isAdult) {
    triggerSocialBar()
    triggerPopunder()
  } else {
    cleanupDynamicAds()
  }
}

// 视口懒加载
const setupIntersection = () => {
  if (!adContainerRef.value) {
    mountNativeAd()
    return
  }

  observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0]
        if (entry && entry.isIntersecting) {
          mountNativeAd()
          observer?.disconnect()
        }
      },
      { rootMargin: '200px 0px' }
  )

  observer.observe(adContainerRef.value)
}

// 监听状态变化（文章从普通变为 NSFW 或反之）
watch(
    () => props.isNsfw,
    (val) => {
      evaluateAds(Boolean(val))
    }
)

onMounted(() => {
  setupIntersection()
  evaluateAds(Boolean(props.isNsfw))
})

onBeforeUnmount(() => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
  cleanupDynamicAds()
})
</script>

<style scoped>
.adsterra-native-wrapper {
  width: 100%;
  margin: 1.5rem auto;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: opacity 0.3s ease;
}

.adsterra-native-box {
  width: 100%;
  max-width: 728px;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  border-radius: 8px;
}

.adsterra-sandbox-frame {
  width: 100%;
  min-height: 120px;
  border: none;
  overflow: hidden;
  display: block;
}

.is-hidden {
  display: none !important;
}
</style>