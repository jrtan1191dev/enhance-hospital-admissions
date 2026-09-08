import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { bmuQueries, useUpdateBmuConfig } from '../services/queries';
import type { BmuAlgorithmConfig } from '../types/admissions';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Sliders, CheckCircle2, RotateCcw, Sparkles, Layers, ShieldAlert, Users, Zap } from 'lucide-react';

export function BmuConfigRoute() {
  const [success, setSuccess] = useState<string | null>(null);

  // TanStack Query: Fetch current config via bmuQueries.config()
  const { data: config, isLoading } = useQuery(bmuQueries.config());

  // Centralized update mutation hook
  const updateMutation = useUpdateBmuConfig(() => {
    setSuccess('Algorithm configuration weights updated successfully! BMU recommendations will recalculate.');
    setTimeout(() => setSuccess(null), 4000);
  });

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Top Banner */}
      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight flex items-center gap-2">
            <Sliders className="h-6 w-6 text-blue-600" />
            BMU Optimization Weight Configuration
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Real-time constraint tuning for the pure Java heuristic engine and future Timefold solver profiles.
          </p>
        </div>
        <Badge variant="outline" className="text-xs bg-slate-50">
          Syncs Real-Time
        </Badge>
      </div>

      {success && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-lg text-emerald-800 text-sm flex items-center gap-2">
          <CheckCircle2 className="h-4 w-4 text-emerald-600 flex-shrink-0" />
          {success}
        </div>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Scoring Weights & Threshold Sliders</CardTitle>
          <CardDescription>
            Adjust the relative scoring multipliers applied during candidate bed evaluation.
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading || !config ? (
            <div className="p-8 text-center text-slate-500 text-sm">Loading config...</div>
          ) : (
            <ConfigForm
              config={config}
              isSaving={updateMutation.isPending}
              onSave={(data) => updateMutation.mutate(data)}
            />
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function ConfigForm({
  config,
  isSaving,
  onSave,
}: {
  config: BmuAlgorithmConfig;
  isSaving: boolean;
  onSave: (data: Partial<BmuAlgorithmConfig>) => void;
}) {
  const [specialtyWeight, setSpecialtyWeight] = useState(config.specialtyMatchWeight ?? 40);
  const [consolidationWeight, setConsolidationWeight] = useState(config.consolidationWeight ?? 30);
  const [fallRiskWeight, setFallRiskWeight] = useState(config.fallRiskProximityWeight ?? 15);
  const [batchThreshold, setBatchThreshold] = useState(config.batchThreshold ?? 3);

  const handleApplyPreset = (spec: number, cons: number, fall: number, batch: number) => {
    setSpecialtyWeight(spec);
    setConsolidationWeight(cons);
    setFallRiskWeight(fall);
    setBatchThreshold(batch);
  };

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    onSave({
      specialtyMatchWeight: specialtyWeight,
      consolidationWeight: consolidationWeight,
      fallRiskProximityWeight: fallRiskWeight,
      batchThreshold: batchThreshold,
    });
  };

  const handleResetDefaults = () => {
    handleApplyPreset(40, 30, 15, 3);
  };

  return (
    <form onSubmit={handleSave} className="space-y-6">
      {/* Quick Strategy Presets */}
      <div className="p-3 bg-slate-100/80 rounded-xl border border-slate-200">
        <div className="text-xs font-semibold text-slate-600 mb-2 flex items-center gap-1">
          <Zap className="h-3.5 w-3.5 text-amber-500" />
          Quick Hospital Operational Presets:
        </div>
        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => handleApplyPreset(40, 30, 15, 3)}
            className="text-xs bg-white hover:bg-slate-50 cursor-pointer"
          >
            ⚖️ Balanced Routine (40/30/15)
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => handleApplyPreset(15, 60, 10, 2)}
            className="text-xs bg-white hover:bg-amber-50 text-amber-900 border-amber-200 cursor-pointer"
          >
            🚨 ED Surge Decongestion (15/60/10)
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => handleApplyPreset(70, 10, 25, 4)}
            className="text-xs bg-white hover:bg-blue-50 text-blue-900 border-blue-200 cursor-pointer"
          >
            🩺 Specialist Precision (70/10/25)
          </Button>
        </div>
      </div>

      {/* Specialty Match Weight */}
      <div className="space-y-3 p-4 bg-slate-50/70 rounded-xl border border-slate-200">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-lg bg-blue-100 text-blue-700">
              <Sparkles className="h-4 w-4" />
            </div>
            <div>
              <label className="text-sm font-semibold text-slate-900">Specialty Alignment Weight</label>
              <p className="text-xs text-slate-500">Bonus points awarded when bed ward specialty matches admission specialty.</p>
            </div>
          </div>
          <span className="text-sm font-bold text-blue-700 bg-blue-50 px-3 py-1 rounded-lg border border-blue-200 font-mono shadow-2xs">
            +{specialtyWeight} pts
          </span>
        </div>
        <input
          type="range"
          min="0"
          max="100"
          step="5"
          value={specialtyWeight}
          onChange={(e) => setSpecialtyWeight(Number(e.target.value))}
          className="w-full h-2 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-blue-600"
        />
        <div className="flex justify-between text-[10px] text-slate-400 font-mono">
          <span>0 (Flexible Cross-Ward)</span>
          <span>50</span>
          <span>100 (Strict Specialty Lock)</span>
        </div>
      </div>

      {/* Consolidation Weight */}
      <div className="space-y-3 p-4 bg-slate-50/70 rounded-xl border border-slate-200">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-lg bg-emerald-100 text-emerald-700">
              <Layers className="h-4 w-4" />
            </div>
            <div>
              <label className="text-sm font-semibold text-slate-900">Consolidation Packing Weight</label>
              <p className="text-xs text-slate-500">Bonus points for packing into partially filled cubicles to protect all-White flex rooms.</p>
            </div>
          </div>
          <span className="text-sm font-bold text-emerald-700 bg-emerald-50 px-3 py-1 rounded-lg border border-emerald-200 font-mono shadow-2xs">
            +{consolidationWeight} pts
          </span>
        </div>
        <input
          type="range"
          min="0"
          max="100"
          step="5"
          value={consolidationWeight}
          onChange={(e) => setConsolidationWeight(Number(e.target.value))}
          className="w-full h-2 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-emerald-600"
        />
        <div className="flex justify-between text-[10px] text-slate-400 font-mono">
          <span>0 (Spread Out)</span>
          <span>50</span>
          <span>100 (Max Compact Packing)</span>
        </div>
      </div>

      {/* Fall Risk Proximity Weight */}
      <div className="space-y-3 p-4 bg-slate-50/70 rounded-xl border border-slate-200">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-lg bg-amber-100 text-amber-700">
              <ShieldAlert className="h-4 w-4" />
            </div>
            <div>
              <label className="text-sm font-semibold text-slate-900">Fall Risk Nursing Proximity Weight</label>
              <p className="text-xs text-slate-500">Bonus points awarded when high fall-risk patients are placed in beds adjacent to nursing station.</p>
            </div>
          </div>
          <span className="text-sm font-bold text-amber-700 bg-amber-50 px-3 py-1 rounded-lg border border-amber-200 font-mono shadow-2xs">
            +{fallRiskWeight} pts
          </span>
        </div>
        <input
          type="range"
          min="0"
          max="100"
          step="5"
          value={fallRiskWeight}
          onChange={(e) => setFallRiskWeight(Number(e.target.value))}
          className="w-full h-2 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-amber-600"
        />
        <div className="flex justify-between text-[10px] text-slate-400 font-mono">
          <span>0 (Ignore Proximity)</span>
          <span>50</span>
          <span>100 (Prioritize Safety First)</span>
        </div>
      </div>

      {/* Batch Holding Threshold */}
      <div className="space-y-3 p-4 bg-slate-50/70 rounded-xl border border-slate-200">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-lg bg-purple-100 text-purple-700">
              <Users className="h-4 w-4" />
            </div>
            <div>
              <label className="text-sm font-semibold text-slate-900">Dynamic Batching Holding Threshold</label>
              <p className="text-xs text-slate-500">Minimum waiting cluster size required to trigger proactive holding room conversion.</p>
            </div>
          </div>
          <span className="text-sm font-bold text-purple-700 bg-purple-50 px-3 py-1 rounded-lg border border-purple-200 font-mono shadow-2xs">
            {batchThreshold} patients
          </span>
        </div>
        <input
          type="range"
          min="2"
          max="6"
          step="1"
          value={batchThreshold}
          onChange={(e) => setBatchThreshold(Number(e.target.value))}
          className="w-full h-2 bg-slate-200 rounded-lg appearance-none cursor-pointer accent-purple-600"
        />
        <div className="flex justify-between text-[10px] text-slate-400 font-mono">
          <span>2 (Eager Batching)</span>
          <span>4</span>
          <span>6 (High Threshold)</span>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex items-center justify-between pt-4 border-t border-slate-200">
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={handleResetDefaults}
          className="text-xs text-slate-600 cursor-pointer"
        >
          <RotateCcw className="h-3.5 w-3.5 mr-1" />
          Reset Defaults
        </Button>

        <Button
          type="submit"
          size="sm"
          disabled={isSaving}
          className="bg-blue-600 hover:bg-blue-700 text-white font-medium text-xs px-6 cursor-pointer shadow-xs"
        >
          {isSaving ? 'Saving...' : 'Save Configuration (1-Click)'}
        </Button>
      </div>
    </form>
  );
}
