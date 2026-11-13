import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { MatButton } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIcon } from '@angular/material/icon';

import { AuthService } from '../../core/auth/auth.service';
import { Info } from '../../core/info';

/**
 * Temporary page: checks that the frontend reaches the backend and that the
 * backend reaches the database.
 */
@Component({
  selector: 'app-home',
  imports: [DatePipe, MatButton, MatCardModule, MatIcon],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  protected readonly auth = inject(AuthService);
  /** httpResource runs the GET and exposes value(), isLoading(), error() as signals. */
  protected readonly info = httpResource<Info>(() => '/api/info');
}
