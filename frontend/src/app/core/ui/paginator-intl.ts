import { Injectable } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';

/** Texts of the table paginator, translated. */
@Injectable()
export class TranslatedPaginatorIntl extends MatPaginatorIntl {
  override itemsPerPageLabel = $localize`:@@paginator.itemsPerPage:Rows per page`;
  override nextPageLabel = $localize`:@@paginator.next:Next page`;
  override previousPageLabel = $localize`:@@paginator.previous:Previous page`;
  override firstPageLabel = $localize`:@@paginator.first:First page`;
  override lastPageLabel = $localize`:@@paginator.last:Last page`;
  override getRangeLabel = (page: number, size: number, total: number): string => {
    if (total === 0) return $localize`:@@paginator.empty:0 of 0`;
    const start = page * size + 1;
    const end = Math.min(start + size - 1, total);
    return $localize`:@@paginator.range:${start}:start:–${end}:end: of ${total}:total:`;
  };
}
