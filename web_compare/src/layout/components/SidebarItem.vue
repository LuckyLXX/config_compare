<template>
  <div v-if="!item.meta || !item.meta.hidden">
    <!-- 只有一个子路由且需要显示在菜单中 -->
    <template v-if="hasOneShowingChild(item.children, item) && (!onlyOneChild.children || onlyOneChild.noShowingChildren)">
      <app-link v-if="onlyOneChild.meta" :to="resolvePath(onlyOneChild.path)">
        <el-menu-item :index="resolvePath(onlyOneChild.path)" :class="{'submenu-title-noDropdown': !isNest}">
          <el-icon v-if="onlyOneChild.meta.icon">
            <component :is="onlyOneChild.meta.icon" />
          </el-icon>
          <template #title>
            <span>{{ onlyOneChild.meta.title }}</span>
          </template>
        </el-menu-item>
      </app-link>
    </template>

    <!-- 有多个子路由 -->
    <el-sub-menu v-else ref="subMenu" :index="resolvePath(item.path)">
      <template #title>
        <el-icon v-if="item.meta && item.meta.icon">
          <component :is="item.meta.icon" />
        </el-icon>
        <span v-if="item.meta && item.meta.title">{{ item.meta.title }}</span>
      </template>
      <sidebar-item
        v-for="child in visibleChildren"
        :key="child.path"
        :is-nest="true"
        :item="child"
        :base-path="child.path"
        class="nest-menu"
      />
    </el-sub-menu>
  </div>
</template>

<script>
import { computed, ref } from 'vue'
import { isExternal } from '@/utils/validate'
import AppLink from './AppLink.vue'

export default {
  name: 'SidebarItem',
  components: { AppLink },
  props: {
    // 路由对象
    item: {
      type: Object,
      required: true
    },
    // 是否嵌套
    isNest: {
      type: Boolean,
      default: false
    },
    // 基础路径
    basePath: {
      type: String,
      default: ''
    }
  },
  setup(props) {
    const onlyOneChild = ref({})

    // 过滤可见的子路由
    const visibleChildren = computed(() => {
      return props.item.children ? props.item.children.filter(item => !item.meta || !item.meta.hidden) : []
    })

    const hasOneShowingChild = (children = [], parent) => {
      const showingChildren = visibleChildren.value

      // 只有一个子路由时，直接显示该子路由
      if (showingChildren.length === 1) {
        onlyOneChild.value = showingChildren[0]
        return true
      }

      // 如果没有子路由，则显示父路由
      if (showingChildren.length === 0) {
        onlyOneChild.value = { ...parent, path: '', noShowingChildren: true }
        return true
      }

      return false
    }

    const resolvePath = (routePath) => {
      if (isExternal(routePath)) {
        return routePath
      }
      if (isExternal(props.basePath)) {
        return props.basePath
      }
      // 如果routePath已经是完整路径（以/开头），直接返回
      if (routePath && routePath.startsWith('/')) {
        return routePath
      }
      // 否则拼接basePath和routePath
      return props.basePath ? `${props.basePath}/${routePath}`.replace(/\/+/g, '/') : routePath
    }

    return {
      onlyOneChild,
      visibleChildren,
      hasOneShowingChild,
      resolvePath
    }
  }
}
</script>