import { Component } from '@angular/core';
import { DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Button } from 'primeng/button';
import { Card } from 'primeng/card';
import { Tag } from 'primeng/tag';

import { Info } from '../../core/info';

/**
 * Pagina provvisoria: verifica che il frontend raggiunga il backend
 * e che il backend raggiunga il database.
 */
@Component({
  selector: 'app-home',
  imports: [DatePipe, Button, Card, Tag],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  /** httpResource esegue la GET e espone value(), isLoading(), error() come signal. */
  protected readonly info = httpResource<Info>(() => '/api/info');
}
