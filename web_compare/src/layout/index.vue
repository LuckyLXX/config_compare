<template>
  <div class="app-wrapper">
    <el-container class="layout-container">
      <!-- 侧边栏 -->
      <el-aside :width="isCollapse ? '64px' : '210px'" class="sidebar-container">
        <div class="sidebar-logo" :class="{ collapse: isCollapse }">
          <img src="/favicon.ico" alt="logo" class="sidebar-logo-img" />
          <h1 v-show="!isCollapse" class="sidebar-title">配置比对系统</h1>
        </div>
        <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :unique-opened="true"
          :collapse-transition="false"
          mode="vertical"
          class="sidebar-menu"
          router
        >
          <sidebar-item
            v-for="route in routes"
            :key="route.path"
            :item="route"
            :base-path="route.path"
          />
        </el-menu>
      </el-aside>

      <el-container>
        <!-- 头部 -->
        <el-header class="navbar">
          <div class="navbar-left">
            <el-icon 
              :size="18" 
              class="hamburger" 
              @click="toggleSidebar"
            >
              <component :is="isCollapse ? 'Expand' : 'Fold'" />
            </el-icon>
            
            <el-breadcrumb class="app-breadcrumb" separator="/">
              <el-breadcrumb-item
                v-for="(item, index) in levelList"
                :key="item.path"
              >
                <span
                  v-if="item.redirect === 'noRedirect' || index === levelList.length - 1"
                  class="no-redirect"
                >{{ item.meta.title }}</span>
                <a v-else @click.prevent="handleLink(item)">{{ item.meta.title }}</a>
              </el-breadcrumb-item>
            </el-breadcrumb>
          </div>

          <div class="navbar-right">
            <el-dropdown class="avatar-container" trigger="click">
              <div class="avatar-wrapper">
                <el-avatar :size="32" :src="avatarUrl">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <el-icon class="caret-down">
                  <CaretBottom />
                </el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item>个人中心</el-dropdown-item>
                  <el-dropdown-item divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <!-- 主内容区 -->
        <el-main class="app-main">
          <div class="app-main-container">
            <router-view v-slot="{ Component, route }">
              <transition name="fade-transform" mode="out-in">
                <keep-alive>
                  <component :is="Component" :key="route.path" />
                </keep-alive>
              </transition>
            </router-view>
          </div>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SidebarItem from './components/SidebarItem.vue'

export default {
  name: 'Layout',
  components: {
    SidebarItem
  },
  setup() {
    const route = useRoute()
    const router = useRouter()

    const isCollapse = ref(false)
    const avatarUrl = ref('')

    // 获取路由配置 - 只获取顶级路由避免重复
    const routes = computed(() => {
      // 直接导入路由配置，避免使用 router.getRoutes() 导致的重复
      return [
        {
          path: '/dashboard',
          meta: { title: '概览', icon: 'House' },
          children: []
        },
        {
          path: '/system',
          meta: { title: '系统管理', icon: 'Setting' },
          children: [
            {
              path: '/system/info',
              meta: { title: '系统信息', icon: 'Monitor' }
            },
            {
              path: '/system/servers',
              meta: { title: '服务器管理', icon: 'Cpu' }
            }
          ]
        },
        {
          path: '/baseline',
          meta: { title: '基线管理', icon: 'Document' },
          children: [
            {
              path: '/baseline/manage',
              meta: { title: '基线版本', icon: 'FolderOpened' }
            },
            {
              path: '/baseline/category',
              meta: { title: '配置分类', icon: 'Menu' }
            }
          ]
        },
        {
          path: '/collect',
          meta: { title: '采集中心', icon: 'Download' },
          children: [
            {
              path: '/collect/tasks',
              meta: { title: '采集任务', icon: 'List' }
            },
            {
              path: '/collect/templates',
              meta: { title: '采集模板', icon: 'DocumentCopy' }
            }
          ]
        },
        {
          path: '/compare',
          meta: { title: '比对中心', icon: 'Operation' },
          children: [
            {
              path: '/compare/tasks',
              meta: { title: '比对任务', icon: 'Switch' }
            },
            {
              path: '/compare/results',
              meta: { title: '比对结果', icon: 'DataAnalysis' }
            }
          ]
        },
        {
          path: '/report',
          meta: { title: '报告中心', icon: 'TrendCharts' },
          children: [
            {
              path: '/report/dashboard',
              meta: { title: '仪表板', icon: 'DataBoard' }
            },
            {
              path: '/report/schedule',
              meta: { title: '任务调度', icon: 'Timer' }
            },
            {
              path: '/report/execution',
              meta: { title: '执行报告', icon: 'Document' }
            },
            {
              path: '/report/diff-analysis',
              meta: { title: '差异分析', icon: 'TrendCharts' }
            },
            {
              path: '/report/system-health',
              meta: { title: '系统健康', icon: 'CircleCheck' }
            },
            {
              path: '/report/collect-stats',
              meta: { title: '采集统计', icon: 'PieChart' }
            },
            {
              path: '/report/compare-stats',
              meta: { title: '比对统计', icon: 'Histogram' }
            }
          ]
        }
      ]
    })

    // 当前激活的菜单
    const activeMenu = computed(() => {
      const { meta, path } = route
      if (meta.activeMenu) {
        return meta.activeMenu
      }
      return path
    })

    // 面包屑导航
    const levelList = ref([])

    const getBreadcrumb = () => {
      let matched = route.matched.filter(item => item.meta && item.meta.title)
      const first = matched[0]

      if (!isDashboard(first)) {
        matched = [{ path: '/dashboard', meta: { title: '概览' } }].concat(matched)
      }

      levelList.value = matched.filter(item => 
        item.meta && item.meta.title && item.meta.breadcrumb !== false
      )
    }

    const isDashboard = (route) => {
      const name = route && route.name
      if (!name) {
        return false
      }
      return name.trim().toLocaleLowerCase() === 'Dashboard'.toLocaleLowerCase()
    }

    const handleLink = (item) => {
      const { redirect, path } = item
      if (redirect) {
        router.push(redirect)
        return
      }
      router.push(path)
    }

    const toggleSidebar = () => {
      isCollapse.value = !isCollapse.value
    }

    watch(route, getBreadcrumb, { immediate: true })

    return {
      isCollapse,
      avatarUrl,
      routes,
      activeMenu,
      levelList,
      toggleSidebar,
      handleLink
    }
  }
}
</script>

<style lang="scss" scoped>
.app-wrapper {
  position: relative;
  height: 100vh;
  width: 100%;
}

.layout-container {
  height: 100%;
}

.sidebar-container {
  background-color: #304156;
  transition: width 0.28s;
  height: 100vh;
  position: relative;

  .sidebar-logo {
    display: flex;
    align-items: center;
    padding: 20px;
    height: 50px;
    background-color: #2b2f3a;
    
    &.collapse {
      padding: 20px 10px;
    }

    .sidebar-logo-img {
      width: 32px;
      height: 32px;
      margin-right: 12px;
    }

    .sidebar-title {
      margin: 0;
      color: #fff;
      font-weight: 600;
      font-size: 14px;
    }
  }

  .sidebar-menu {
    border: none;
    height: calc(100% - 90px);
    background-color: #304156;

    :deep(.el-menu-item) {
      color: #bfcbd9;
      background-color: #304156;
      
      &:hover,
      &.is-active {
        background-color: #263445 !important;
        color: #409eff;
      }
    }

    :deep(.el-sub-menu__title) {
      color: #bfcbd9;
      background-color: #304156;
      
      &:hover {
        background-color: #263445 !important;
        color: #409eff;
      }
    }

    // 修复子菜单背景色问题
    :deep(.el-sub-menu .el-menu) {
      background-color: #1f2d3d;
    }

    :deep(.el-sub-menu .el-menu-item) {
      background-color: #1f2d3d !important;
      color: #bfcbd9;
      
      &:hover,
      &.is-active {
        background-color: #001528 !important;
        color: #409eff;
      }
    }

    // 确保所有子菜单项都有正确的背景色
    :deep(.nest-menu .el-menu-item) {
      background-color: #1f2d3d !important;
      color: #bfcbd9;
      
      &:hover,
      &.is-active {
        background-color: #001528 !important;
        color: #409eff;
      }
    }
  }
}

.navbar {
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;

  .navbar-left {
    display: flex;
    align-items: center;

    .hamburger {
      cursor: pointer;
      margin-right: 20px;
      transition: color 0.2s;

      &:hover {
        color: #409eff;
      }
    }

    .app-breadcrumb {
      line-height: 50px;
    }
  }

  .navbar-right {
    .avatar-container {
      cursor: pointer;
      
      .avatar-wrapper {
        display: flex;
        align-items: center;
        
        .caret-down {
          margin-left: 8px;
          font-size: 12px;
        }
      }
    }
  }
}

.app-main {
  background-color: #f0f2f5;
  padding: 20px;
  
  .app-main-container {
    background: #fff;
    padding: 20px;
    border-radius: 4px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.12), 0 0 6px rgba(0, 0, 0, 0.04);
    min-height: calc(100vh - 110px);
  }
}

// 过渡动画
.fade-transform-leave-active,
.fade-transform-enter-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>