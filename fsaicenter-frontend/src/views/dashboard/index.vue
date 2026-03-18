<template>
  <div class="dashboard">
    <a-row :gutter="16">
      <a-col :span="6">
        <a-card>
          <a-statistic title="今日请求数" :value="stats.todayRequests" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="成功率" :value="stats.successRate" :precision="2" suffix="%" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="总消费" :value="stats.totalCost" :precision="2" prefix="¥" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="活跃 API Key" :value="stats.activeApiKeys" />
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="16">
        <a-card title="请求趋势（近7天）">
          <v-chart :option="trendOption" style="height: 320px" autoresize />
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="模型调用分布">
          <v-chart :option="pieOption" style="height: 320px" autoresize />
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="16">
        <a-card title="最近请求日志">
          <a-table :columns="logColumns" :data="recentLogs" :pagination="false" size="small">
            <template #status="{ record }">
              <a-tag :color="record.status === 1 ? 'green' : 'red'">
                {{ record.status === 1 ? '成功' : '失败' }}
              </a-tag>
            </template>
          </a-table>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="热门模型 Top 5">
          <div v-for="(item, index) in topModels" :key="index" class="top-model-item">
            <span class="rank" :class="'rank-' + index">{{ index + 1 }}</span>
            <span class="name">{{ item.modelName }}</span>
            <span class="count">{{ item.callCount }} 次</span>
          </div>
          <a-empty v-if="topModels.length === 0" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { dashboardApi } from '@/api/dashboard'
import type { DashboardStats, DashboardTrend, ModelDistribution, TopModel, RecentLog } from '@/types/dashboard'

use([CanvasRenderer, LineChart, PieChart, GridComponent, TooltipComponent, LegendComponent])

const stats = reactive<DashboardStats>({
  todayRequests: 0,
  successRate: 0,
  totalCost: 0,
  activeApiKeys: 0
})

const trendData = ref<DashboardTrend>({ xAxis: [], series: [] })
const distributionData = ref<ModelDistribution[]>([])
const topModels = ref<TopModel[]>([])
const recentLogs = ref<RecentLog[]>([])

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: {},
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category', data: trendData.value.xAxis },
  yAxis: { type: 'value' },
  series: trendData.value.series.map(s => ({
    name: s.name,
    type: 'line',
    smooth: true,
    data: s.data
  }))
}))

const pieOption = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    data: distributionData.value.map(d => ({ name: d.name, value: d.value })),
    emphasis: { itemStyle: { shadowBlur: 10 } }
  }]
}))

const logColumns = [
  { title: '时间', dataIndex: 'time', width: 160 },
  { title: '模型', dataIndex: 'model' },
  { title: 'API Key', dataIndex: 'apiKey', ellipsis: true },
  { title: '状态', slotName: 'status', width: 80 },
  { title: '耗时', dataIndex: 'duration', width: 100 }
]

const fetchData = async () => {
  try {
    const [statsRes, trendRes, distRes, topRes, logsRes] = await Promise.all([
      dashboardApi.getStats(),
      dashboardApi.getRequestTrend(7),
      dashboardApi.getModelDistribution(),
      dashboardApi.getTopModels(5),
      dashboardApi.getRecentLogs(10)
    ])
    Object.assign(stats, statsRes)
    trendData.value = trendRes
    distributionData.value = distRes
    topModels.value = topRes
    recentLogs.value = logsRes
  } catch (error) {
    console.error('加载仪表盘数据失败:', error)
  }
}

onMounted(fetchData)
</script>

<style scoped>
.dashboard {
  padding: 20px;
}
.top-model-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}
.top-model-item:last-child {
  border-bottom: none;
}
.rank {
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 50%;
  background: #e8f3ff;
  color: #165dff;
  font-size: 12px;
  font-weight: bold;
  margin-right: 12px;
  flex-shrink: 0;
}
.rank-0 { background: #ff7d00; color: #fff; }
.rank-1 { background: #86909c; color: #fff; }
.rank-2 { background: #c9cdd4; color: #fff; }
.name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.count {
  color: #86909c;
  font-size: 13px;
  flex-shrink: 0;
}
</style>
