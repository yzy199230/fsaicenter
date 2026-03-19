import { useState, useEffect, useMemo } from 'react'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { Command, CommandInput, CommandList, CommandEmpty, CommandGroup, CommandItem } from '@/components/ui/command'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { modelApi } from '@/api/model'
import type { AiModel } from '@/types/model'
import { Check, ChevronsUpDown, X, Loader2 } from 'lucide-react'
import { cn } from '@/lib/utils'

// ---------------------------------------------------------------------------
// Type label mapping (consistent with ModelsPage)
// ---------------------------------------------------------------------------

const TYPE_LABELS: Record<string, string> = {
  Chat: '对话',
  Embedding: '向量',
  Image: '图片生成',
  ASR: '语音识别',
  TTS: '语音合成',
  Video: '视频生成',
  ImageRecognition: '图片识别',
}

function typeLabel(type: string): string {
  return TYPE_LABELS[type] ?? type
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

interface ModelMultiSelectProps {
  value: number[]
  onChange: (ids: number[]) => void
}

export function ModelMultiSelect({ value, onChange }: ModelMultiSelectProps) {
  const [open, setOpen] = useState(false)
  const [models, setModels] = useState<AiModel[]>([])
  const [loading, setLoading] = useState(false)

  // Fetch models when popover opens for the first time
  useEffect(() => {
    if (open && models.length === 0) {
      setLoading(true)
      modelApi
        .getList()
        .then(setModels)
        .catch(() => {})
        .finally(() => setLoading(false))
    }
  }, [open, models.length])

  // Group models by type
  const grouped = useMemo(() => {
    const map = new Map<string, AiModel[]>()
    for (const m of models) {
      const key = m.type || 'Other'
      if (!map.has(key)) map.set(key, [])
      map.get(key)!.push(m)
    }
    return map
  }, [models])

  // Quick lookup for selected model info
  const selectedModels = useMemo(
    () => models.filter((m) => value.includes(m.id)),
    [models, value],
  )

  const toggle = (id: number) => {
    if (value.includes(id)) {
      onChange(value.filter((v) => v !== id))
    } else {
      onChange([...value, id])
    }
  }

  const remove = (id: number) => {
    onChange(value.filter((v) => v !== id))
  }

  return (
    <div className="space-y-2">
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <Button
            variant="outline"
            role="combobox"
            aria-expanded={open}
            className="w-full justify-between bg-white/[0.03] border-white/[0.06] hover:bg-white/[0.06] text-sm font-normal h-9"
          >
            <span className="truncate text-muted-foreground">
              {value.length > 0
                ? `已选择 ${value.length} 个模型`
                : '选择模型...'}
            </span>
            <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
          </Button>
        </PopoverTrigger>

        <PopoverContent className="w-[--radix-popover-trigger-width] p-0" align="start">
          <Command>
            <CommandInput placeholder="搜索模型名称或类型..." />
            <CommandList>
              {loading ? (
                <div className="flex items-center justify-center py-6 gap-2 text-sm text-muted-foreground">
                  <Loader2 className="w-4 h-4 animate-spin" />
                  加载中...
                </div>
              ) : (
                <>
                  <CommandEmpty>未找到匹配模型</CommandEmpty>
                  {Array.from(grouped.entries()).map(([type, items]) => (
                    <CommandGroup
                      key={type}
                      heading={typeLabel(type)}
                    >
                      {items.map((model) => {
                        const selected = value.includes(model.id)
                        return (
                          <CommandItem
                            key={model.id}
                            value={`${model.name} ${model.code} ${typeLabel(type)}`}
                            onSelect={() => toggle(model.id)}
                          >
                            <div
                              className={cn(
                                'mr-2 flex h-4 w-4 items-center justify-center rounded-sm border border-white/[0.2]',
                                selected
                                  ? 'bg-primary border-primary text-primary-foreground'
                                  : 'opacity-50',
                              )}
                            >
                              {selected && <Check className="h-3 w-3" />}
                            </div>
                            <span className="flex-1 truncate">{model.name}</span>
                            <span className="text-[10px] text-muted-foreground ml-2 font-mono">
                              {model.code}
                            </span>
                          </CommandItem>
                        )
                      })}
                    </CommandGroup>
                  ))}
                </>
              )}
            </CommandList>
          </Command>
        </PopoverContent>
      </Popover>

      {/* Selected model tags */}
      {selectedModels.length > 0 ? (
        <div className="flex flex-wrap gap-1.5">
          {selectedModels.map((m) => (
            <Badge
              key={m.id}
              variant="outline"
              className="text-[10px] px-2 py-0.5 h-5 text-muted-foreground border-white/[0.08] gap-1 pr-1"
            >
              {m.name}
              <button
                type="button"
                className="ml-0.5 rounded-full hover:bg-white/[0.1] p-0.5"
                onClick={() => remove(m.id)}
              >
                <X className="h-2.5 w-2.5" />
              </button>
            </Badge>
          ))}
        </div>
      ) : (
        <p className="text-[10px] text-zinc-600">未限制，所有模型均可访问</p>
      )}
    </div>
  )
}
