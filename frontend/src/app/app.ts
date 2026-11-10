import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/** Guscio dell'applicazione: barra superiore + pagina corrente. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {}
