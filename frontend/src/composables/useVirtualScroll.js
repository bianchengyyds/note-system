import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'

/**
 * 虚拟滚动 Hook
 * 用于优化大型文档的渲染性能
 * @param {ref} containerRef - 容器元素引用
 * @param {Array} items - 数据列表
 * @param {number} itemHeight - 每项高度（像素）
 * @param {number} bufferSize - 缓冲区大小（前后额外渲染的项目数）
 */
export function useVirtualScroll(containerRef, items, itemHeight = 50, bufferSize = 5) {
  const scrollTop = ref(0)
  const containerHeight = ref(0)
  
  // 计算可见区域的项目范围
  const visibleRange = computed(() => {
    if (!containerHeight.value) return { start: 0, end: 0 }
    
    const start = Math.floor(scrollTop.value / itemHeight)
    const visibleCount = Math.ceil(containerHeight.value / itemHeight)
    const end = Math.min(start + visibleCount, items.value.length)
    
    return {
      start: Math.max(0, start - bufferSize),
      end: Math.min(items.value.length, end + bufferSize)
    }
  })
  
  // 可见项目列表
  const visibleItems = computed(() => {
    const { start, end } = visibleRange.value
    return items.value.slice(start, end).map((item, index) => ({
      ...item,
      _virtualIndex: start + index
    }))
  })
  
  // 总高度
  const totalHeight = computed(() => items.value.length * itemHeight)
  
  // 偏移量
  const offsetY = computed(() => visibleRange.value.start * itemHeight)
  
  // 处理滚动事件
  const handleScroll = (event) => {
    scrollTop.value = event.target.scrollTop
  }
  
  // 更新容器高度
  const updateContainerHeight = () => {
    if (containerRef.value) {
      containerHeight.value = containerRef.value.clientHeight
    }
  }
  
  // 监听容器大小变化
  onMounted(() => {
    updateContainerHeight()
    window.addEventListener('resize', updateContainerHeight)
    
    if (containerRef.value) {
      containerRef.value.addEventListener('scroll', handleScroll)
    }
  })
  
  onBeforeUnmount(() => {
    window.removeEventListener('resize', updateContainerHeight)
    if (containerRef.value) {
      containerRef.value.removeEventListener('scroll', handleScroll)
    }
  })
  
  // 滚动到指定位置
  const scrollTo = (index) => {
    if (containerRef.value) {
      containerRef.value.scrollTop = index * itemHeight
    }
  }
  
  // 滚动到顶部
  const scrollToTop = () => {
    scrollTo(0)
  }
  
  // 滚动到底部
  const scrollToBottom = () => {
    scrollTo(items.value.length - 1)
  }
  
  return {
    visibleItems,
    totalHeight,
    offsetY,
    scrollTo,
    scrollToTop,
    scrollToBottom
  }
}
