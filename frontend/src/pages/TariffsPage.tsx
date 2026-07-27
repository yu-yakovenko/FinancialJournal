import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { api, formatUah } from '../api/client';
import { SERVICE_TYPE_LABELS } from '../api/types';
import type { ServiceType, TariffPlan, TariffRate } from '../api/types';

export function TariffsPage() {
  const [plans, setPlans] = useState<TariffPlan[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [historyForPlanId, setHistoryForPlanId] = useState<number | null>(null);
  const [changingPriceFor, setChangingPriceFor] = useState<TariffPlan | null>(null);
  const [editingPlan, setEditingPlan] = useState<TariffPlan | null>(null);

  function reload() {
    api.tariffPlans().then(setPlans).catch((e) => setError(e.message));
  }

  useEffect(reload, []);

  async function toggleActive(plan: TariffPlan) {
    await api.updateTariffPlan(plan.id, { label: plan.label, active: !plan.active });
    reload();
  }

  return (
    <div>
      <h2>Тарифи</h2>
      <div className="notice notice-blue">
        <span className="notice-icon">i</span>
        <span>Зміна ціни не переписує минуле — вона діє з дати "чинний з", а старі місяці журналу й далі рахуються за старою ціною.</span>
      </div>
      {error && <div className="error-banner">{error}</div>}

      <div className="toolbar">
        <button className="primary" onClick={() => setShowCreateForm(true)}>+ Новий тариф</button>
      </div>

      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Назва</th>
              <th>Тип послуги</th>
              <th>Поточна ціна</th>
              <th>Статус</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {plans.map((plan) => (
              <tr key={plan.id}>
                <td>{plan.label}</td>
                <td className="muted">{SERVICE_TYPE_LABELS[plan.serviceType]}</td>
                <td>{plan.currentAmountKopiykas != null ? `${formatUah(plan.currentAmountKopiykas)} грн` : '—'}</td>
                <td>
                  <span className={`status-badge ${plan.active ? 'status-green' : 'status-red'}`}>
                    <span className="dot" />
                    {plan.active ? 'Активний' : 'Неактивний'}
                  </span>
                </td>
                <td>
                  <button onClick={() => setEditingPlan(plan)}>Редагувати</button>{' '}
                  <button onClick={() => setChangingPriceFor(plan)}>Змінити ціну</button>{' '}
                  <button onClick={() => setHistoryForPlanId(plan.id)}>Історія цін</button>{' '}
                  <button onClick={() => toggleActive(plan)}>{plan.active ? 'Деактивувати' : 'Активувати'}</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showCreateForm && (
        <CreateTariffPlanForm
          onSubmit={async (data) => {
            await api.createTariffPlan(data);
            setShowCreateForm(false);
            reload();
          }}
          onCancel={() => setShowCreateForm(false)}
        />
      )}

      {editingPlan && (
        <EditTariffPlanForm
          plan={editingPlan}
          onSubmit={async (label) => {
            await api.updateTariffPlan(editingPlan.id, { label, active: editingPlan.active });
            setEditingPlan(null);
            reload();
          }}
          onCancel={() => setEditingPlan(null)}
        />
      )}

      {changingPriceFor && (
        <ChangePriceForm
          plan={changingPriceFor}
          onSubmit={async (data) => {
            await api.addTariffRate(changingPriceFor.id, data);
            setChangingPriceFor(null);
            reload();
          }}
          onCancel={() => setChangingPriceFor(null)}
        />
      )}

      {historyForPlanId !== null && (
        <PriceHistoryModal planId={historyForPlanId} onClose={() => setHistoryForPlanId(null)} onRateChanged={reload} />
      )}
    </div>
  );
}

function EditTariffPlanForm({
  plan,
  onSubmit,
  onCancel,
}: {
  plan: TariffPlan;
  onSubmit: (label: string) => Promise<void>;
  onCancel: () => void;
}) {
  const [label, setLabel] = useState(plan.label);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await onSubmit(label.trim());
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <form className="modal" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <h3>Редагувати тариф</h3>
        {error && <div className="error-banner">{error}</div>}

        <div className="form-row">
          <label htmlFor="editLabel">Назва</label>
          <input id="editLabel" value={label} onChange={(e) => setLabel(e.target.value)} required />
        </div>

        <div className="toolbar">
          <button type="submit" className="primary" disabled={saving}>{saving ? 'Збереження…' : 'Зберегти'}</button>
          <button type="button" onClick={onCancel}>Скасувати</button>
        </div>
      </form>
    </div>
  );
}

function CreateTariffPlanForm({
  onSubmit,
  onCancel,
}: {
  onSubmit: (data: { serviceType: ServiceType; label: string; initialAmountUah: number; effectiveFrom?: string }) => Promise<void>;
  onCancel: () => void;
}) {
  const [serviceType, setServiceType] = useState<ServiceType>('INDIVIDUAL');
  const [label, setLabel] = useState('');
  const [amountUah, setAmountUah] = useState('');
  const [effectiveFrom, setEffectiveFrom] = useState(() => new Date().toISOString().slice(0, 10));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await onSubmit({ serviceType, label: label.trim(), initialAmountUah: Number(amountUah), effectiveFrom });
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <form className="modal" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <h3>Новий тариф</h3>
        {error && <div className="error-banner">{error}</div>}

        <div className="form-row">
          <label htmlFor="serviceType">Тип послуги</label>
          <select id="serviceType" value={serviceType} onChange={(e) => setServiceType(e.target.value as ServiceType)}>
            <option value="INDIVIDUAL">{SERVICE_TYPE_LABELS.INDIVIDUAL}</option>
            <option value="GROUP">{SERVICE_TYPE_LABELS.GROUP}</option>
            <option value="CHOIR">{SERVICE_TYPE_LABELS.CHOIR}</option>
            <option value="ACQUIRING">{SERVICE_TYPE_LABELS.ACQUIRING}</option>
          </select>
        </div>

        <div className="form-row">
          <label htmlFor="label">Назва</label>
          <input id="label" value={label} onChange={(e) => setLabel(e.target.value)} placeholder="напр. Хор, стандартний" required />
        </div>

        <div className="form-row">
          <label htmlFor="amount">Ціна, грн</label>
          <input id="amount" type="number" step="0.01" min="0.01" value={amountUah} onChange={(e) => setAmountUah(e.target.value)} required />
        </div>

        <div className="form-row">
          <label htmlFor="effectiveFrom">Чинний з</label>
          <input id="effectiveFrom" type="date" value={effectiveFrom} onChange={(e) => setEffectiveFrom(e.target.value)} required />
        </div>

        <div className="toolbar">
          <button type="submit" className="primary" disabled={saving}>{saving ? 'Збереження…' : 'Створити'}</button>
          <button type="button" onClick={onCancel}>Скасувати</button>
        </div>
      </form>
    </div>
  );
}

function ChangePriceForm({
  plan,
  onSubmit,
  onCancel,
}: {
  plan: TariffPlan;
  onSubmit: (data: { amountUah: number; effectiveFrom?: string }) => Promise<void>;
  onCancel: () => void;
}) {
  const [amountUah, setAmountUah] = useState('');
  const [effectiveFrom, setEffectiveFrom] = useState(() => new Date().toISOString().slice(0, 10));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await onSubmit({ amountUah: Number(amountUah), effectiveFrom });
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <form className="modal" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <h3>Нова ціна: {plan.label}</h3>
        <p className="muted">
          Поточна ціна {plan.currentAmountKopiykas != null ? `${formatUah(plan.currentAmountKopiykas)} грн` : '—'}. Це додасть новий запис в
          історію цін, а не перепише старий — минулі місяці журналу не зміняться.
        </p>
        {error && <div className="error-banner">{error}</div>}

        <div className="form-row">
          <label htmlFor="newAmount">Нова ціна, грн</label>
          <input id="newAmount" type="number" step="0.01" min="0.01" value={amountUah} onChange={(e) => setAmountUah(e.target.value)} required />
        </div>

        <div className="form-row">
          <label htmlFor="newEffectiveFrom">Чинна з</label>
          <input id="newEffectiveFrom" type="date" value={effectiveFrom} onChange={(e) => setEffectiveFrom(e.target.value)} required />
        </div>

        <div className="toolbar">
          <button type="submit" className="primary" disabled={saving}>{saving ? 'Збереження…' : 'Зберегти'}</button>
          <button type="button" onClick={onCancel}>Скасувати</button>
        </div>
      </form>
    </div>
  );
}

function PriceHistoryModal({
  planId,
  onClose,
  onRateChanged,
}: {
  planId: number;
  onClose: () => void;
  onRateChanged: () => void;
}) {
  const [rates, setRates] = useState<TariffRate[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editingRate, setEditingRate] = useState<TariffRate | null>(null);

  function reloadRates() {
    api.tariffRates(planId).then(setRates).catch((e) => setError(e.message));
  }

  useEffect(reloadRates, [planId]);

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>Історія цін</h3>
        {error && <div className="error-banner">{error}</div>}
        {!rates && !error && <p className="muted">Завантаження…</p>}
        {rates && (
          <table>
            <thead>
              <tr>
                <th>Чинний з</th>
                <th>Ціна</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {rates.map((rate) => (
                <tr key={rate.id}>
                  <td>{rate.effectiveFrom}</td>
                  <td>{formatUah(rate.amountKopiykas)} грн</td>
                  <td><button onClick={() => setEditingRate(rate)}>Редагувати</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <div className="toolbar" style={{ marginTop: 16 }}>
          <button onClick={onClose}>Закрити</button>
        </div>
      </div>

      {editingRate && (
        <EditRateForm
          rate={editingRate}
          onSubmit={async (data) => {
            await api.updateTariffRate(editingRate.id, data);
            setEditingRate(null);
            reloadRates();
            onRateChanged();
          }}
          onCancel={() => setEditingRate(null)}
        />
      )}
    </div>
  );
}

function EditRateForm({
  rate,
  onSubmit,
  onCancel,
}: {
  rate: TariffRate;
  onSubmit: (data: { amountUah: number; effectiveFrom: string }) => Promise<void>;
  onCancel: () => void;
}) {
  const [amountUah, setAmountUah] = useState(String(rate.amountKopiykas / 100));
  const [effectiveFrom, setEffectiveFrom] = useState(rate.effectiveFrom);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await onSubmit({ amountUah: Number(amountUah), effectiveFrom });
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <form className="modal" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <h3>Редагувати запис ціни</h3>
        <p className="muted">
          Це виправляє вже створений запис (не додає новий) — використовуй лише для виправлення помилки, а не
          для оголошення нової ціни.
        </p>
        {error && <div className="error-banner">{error}</div>}

        <div className="form-row">
          <label htmlFor="editAmount">Ціна, грн</label>
          <input id="editAmount" type="number" step="0.01" min="0.01" value={amountUah} onChange={(e) => setAmountUah(e.target.value)} required />
        </div>

        <div className="form-row">
          <label htmlFor="editEffectiveFrom">Чинний з</label>
          <input
            id="editEffectiveFrom"
            type="date"
            value={effectiveFrom}
            onChange={(e) => setEffectiveFrom(e.target.value)}
            required
          />
        </div>

        <div className="toolbar">
          <button type="submit" className="primary" disabled={saving}>{saving ? 'Збереження…' : 'Зберегти'}</button>
          <button type="button" onClick={onCancel}>Скасувати</button>
        </div>
      </form>
    </div>
  );
}
