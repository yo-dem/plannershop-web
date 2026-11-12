import { Injectable } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';

/** Testi del paginatore delle tabelle in italiano. */
@Injectable()
export class PaginatoreItaliano extends MatPaginatorIntl {
  override itemsPerPageLabel = 'Righe per pagina';
  override nextPageLabel = 'Pagina successiva';
  override previousPageLabel = 'Pagina precedente';
  override firstPageLabel = 'Prima pagina';
  override lastPageLabel = 'Ultima pagina';
  override getRangeLabel = (pagina: number, dimensione: number, totale: number): string => {
    if (totale === 0) return '0 di 0';
    const inizio = pagina * dimensione + 1;
    const fine = Math.min(inizio + dimensione - 1, totale);
    return `${inizio}–${fine} di ${totale}`;
  };
}
