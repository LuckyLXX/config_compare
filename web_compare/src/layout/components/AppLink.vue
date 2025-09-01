<template>
  <!-- 外部链接 -->
  <a v-if="isExt" :href="to" target="_blank" rel="noopener">
    <slot />
  </a>
  <!-- 内部路由 -->
  <router-link v-else :to="to" #default="{ href, navigate, isActive }">
    <a :href="href" @click="navigate" :class="{ active: isActive }">
      <slot />
    </a>
  </router-link>
</template>

<script>
import { computed } from 'vue'
import { isExternal } from '@/utils/validate'

export default {
  name: 'AppLink',
  props: {
    to: {
      type: String,
      required: true
    }
  },
  setup(props) {
    const isExt = computed(() => {
      return isExternal(props.to)
    })

    return {
      isExt
    }
  }
}
</script>