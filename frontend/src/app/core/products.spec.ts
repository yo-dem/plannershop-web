import { stockLevel } from './products';

describe('stockLevel', () => {
  it('uses the default thresholds: red at 0, orange up to 10', () => {
    const t = { outOfStockThreshold: 0, lowStockThreshold: 10 };
    expect(stockLevel(0, t)).toBe('out');
    expect(stockLevel(1, t)).toBe('low');
    expect(stockLevel(10, t)).toBe('low');
    expect(stockLevel(11, t)).toBe('ok');
  });

  it('uses the thresholds of the business', () => {
    const t = { outOfStockThreshold: 2, lowStockThreshold: 5 };
    expect(stockLevel(2, t)).toBe('out');
    expect(stockLevel(3, t)).toBe('low');
    expect(stockLevel(6, t)).toBe('ok');
  });

  it('equal thresholds: no orange', () => {
    const t = { outOfStockThreshold: 3, lowStockThreshold: 3 };
    expect(stockLevel(3, t)).toBe('out');
    expect(stockLevel(4, t)).toBe('ok');
  });
});
