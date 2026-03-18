<template>
  <div class="billing-records">
    <a-row :gutter="16">
      <a-col :span="6">
        <a-card>
          <a-statistic title="总消费" :value="stats.totalCost" :precision="2" prefix="¥" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="总请求数" :value="stats.totalRequests" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="总用量" :value="stats.totalUsage" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="平均单价" :value="stats.avgUnitPrice" :precision="4" prefix="¥" />
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="14">
        <a-card title="消费趋势（近7天）">
          <v-chart :option="trendOption" style="height: 280px" autoresize />
        </a-card>
      </a-col>
      <a-col :span="10">
        <a-card title="模型消费排行">
          <a-table :columns="costColumns" :data="modelCosts" :pagination="false" size="small">
            <template #cost="{ record }">
              ¥{{ record.totalCost?.toFixed(2) }}
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>

    <a-card title="计费规则" style="margin-top: 16px">
      <a-table :columns="ruleColumns" :data="rules" :loading="rulesLoading" :pagination="rulePagination" @page-change="handleRulePageChange">
        <template #status="{ record }">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">{{ record.status === 1 ? '生效' : '未生效' }}</a-tag>
        </template>
        <template #unitPrice="{ record }">
          <div v-if="record.unitPrice">¥{{ record.unitPrice }}/{{ record.unitAmount }}tokens</div>
          <div v-else>
            <div>输入: ¥{{ record.inputUnitPrice }}/{{ record.unitAmount }}tokens</div>
            <div>输出: ¥{{ record.outputUnitPrice }}/{{ record.unitAmount }}tokens</div>
          </div>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { billingStatsApi, billingRuleApi } from '@/api/billing'
import type { BillingStats, BillingTrend, ModelCost, BillingRule } from '@/types/billing'

use([CanvasRenderer, BarChart, LineChart, GridComponent, TooltipComponent, LegendComponent])

const stats = reactive<BillingStats>({ totalCost: 0, totalRequests: 0, totalUsage: 0, avgUnitPrice: 0 })
const trendData = ref<BillingTrend[]>([])
const modelCosts = ref<ModelCost[]>([])
const rules = ref<BillingRule[]>([])
const rulesLoading = ref(false)
const ruleQuery = reactive({ pageNum: 1, pageSize: 10 })
const rulePagination = reactive({ current: 1, pageSize: 10, total: 0, showTotal: true })

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: {},
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: { type: 'category', data: trendData.value.map(d => d.date) },
  yAxis: [
    { type: 'value', name: '消费(¥)' },
    { type: 'value', name: '请求数' }
  ],
  series: [
    { name: '消费', type: 'bar', data: trendData.value.map(d => d.cost) },
    { name: '请求数', type: 'line', yAxisIndex: 1, smooth: true, data: trendData.value.map(d => d.requests) }
  ]
}))

const costColumns = [
  { title: '模型', dataIndex: 'modelName' },
  { title: '消费', slotName: 'cost', width: 100 },
  { title: '请求数', dataIndex: 'requests', width: 80 }
]

const ruleColumns = [
  { title: 'ID', dataIndex: 'id', width: 60 },
  { title: '模型', dataIndex: 'modelName' },
  { title: '计费类型', dataIndex: 'billingType', width: 100 },
  { title: '单价', slotName: 'unitPrice', width: 200 },
  { title: '生效时间', dataIndex: 'effectiveTime', width: 170 },
  { title: '状态', slotName: 'status', width: 80 }
]

const fetchStats = async () => {
  try {
    const [statsRes, trendRes, costRes] = await Promise.all([
      billingStatsApi.getStats(),
      billingStatsApi.getTrend(7),
      billingStatsApi.getModelCost()
    ])
    Object.assign(stats, statsRes)
    trendData.value = trendRes
    modelCosts.value = costRes
  } catch (e) {
    console.error('获取统计数据失败:', e)
  }
}

const fetchRules = async () => {
  rulesLoading.value = true
  try {
    const res = await billingRuleApi.getPage(ruleQuery)
    rules.value = res.list
    rulePagination.total = res.total
    rulePagination.current = res.pageNum
  } finally {
    rulesLoading.value = false
  }
}

const handleRulePageChange = (page: number) => {
  ruleQuery.pageNum = page
  fetchRules()
}

onMounted(() => { fetchStats(); fetchRules() })
</script>

<style scoped>
.billing-records { padding: 20px; }
</style>
