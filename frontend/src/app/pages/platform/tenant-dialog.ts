import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInput } from '@angular/material/input';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { firstValueFrom } from 'rxjs';

import { errorMessage, fieldErrors } from '../../core/errors';
import { Notice } from '../../core/ui/notice';

export interface TenantDto {
  id: number;
  name: string;
  active: boolean;
  createdAt: string;
  admins: string[];
}

export const TENANTS_API = '/api/platform/tenants';

/**
 * Dialog to create a tenant with its first administrator (data = null) or to
 * change its name and status. Closes with true after a save.
 */
@Component({
  selector: 'app-tenant-dialog',
  imports: [ReactiveFormsModule, MatButton, MatDialogModule, MatFormFieldModule, MatIcon, MatInput, MatSlideToggle, Notice],
  template: `
    <h2 mat-dialog-title>
      @if (tenant) {
        <ng-container i18n="@@tenants.editTitle">Edit business</ng-container>
      } @else {
        <ng-container i18n="@@tenants.newTitle">New business</ng-container>
      }
    </h2>
    <form [formGroup]="form" (ngSubmit)="save()" novalidate>
      <mat-dialog-content>
        @if (error(); as e) {
          <app-notice type="error">{{ e }}</app-notice>
        }
        <div class="form">
          <mat-form-field>
            <mat-label i18n="@@tenants.name">Business name</mat-label>
            <input matInput id="name" formControlName="name" required />
            <mat-error i18n="@@validation.required">Required</mat-error>
          </mat-form-field>

          @if (tenant) {
            <mat-slide-toggle formControlName="active" i18n="@@tenants.activeToggle">Active (when off, nobody can sign in)</mat-slide-toggle>
          } @else {
            <h3 i18n="@@tenants.firstAdmin">First administrator</h3>
            <mat-form-field>
              <mat-label i18n="@@tenants.adminName">Full name</mat-label>
              <input matInput id="adminName" formControlName="adminName" required />
              <mat-error i18n="@@validation.required">Required</mat-error>
            </mat-form-field>
            <mat-form-field>
              <mat-label i18n="@@tenants.adminEmail">Email</mat-label>
              <input matInput id="adminEmail" type="email" formControlName="adminEmail" required />
              <mat-error i18n="@@validation.email">Invalid email</mat-error>
            </mat-form-field>
            <mat-form-field>
              <mat-label i18n="@@tenants.adminPassword">Temporary password</mat-label>
              <input matInput id="adminPassword" formControlName="adminPassword" autocomplete="off" required />
              <mat-hint i18n="@@tenants.adminPasswordHint">Give it to the administrator: they will have to change it at the first sign-in.</mat-hint>
              <mat-error i18n="@@changePassword.minLength">At least 8 characters</mat-error>
            </mat-form-field>
          }
        </div>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button matButton type="button" mat-dialog-close i18n="@@common.cancel">Cancel</button>
        <button matButton="filled" type="submit" [disabled]="sending()">
          <mat-icon>check</mat-icon>
          @if (tenant) {
            <ng-container i18n="@@common.save">Save</ng-container>
          } @else {
            <ng-container i18n="@@common.create">Create</ng-container>
          }
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    h3 {
      margin: 0.5rem 0 0.25rem;
      font: var(--mat-sys-title-small);
    }
    mat-slide-toggle {
      margin: 0.5rem 0;
    }
  `,
})
export class TenantDialog {
  private readonly http = inject(HttpClient);
  private readonly ref = inject<MatDialogRef<TenantDialog, boolean>>(MatDialogRef);
  /** Tenant to edit; null to create a new one. */
  protected readonly tenant = inject<TenantDto | null>(MAT_DIALOG_DATA);

  private readonly fb = inject(FormBuilder).nonNullable;
  protected readonly form = this.tenant
    ? this.fb.group({ name: [this.tenant.name, Validators.required], active: [this.tenant.active] })
    : this.fb.group({
        name: ['', Validators.required],
        adminName: ['', Validators.required],
        adminEmail: ['', [Validators.required, Validators.email]],
        adminPassword: ['', [Validators.required, Validators.minLength(8)]],
      });

  protected readonly sending = signal(false);
  protected readonly error = signal<string | null>(null);

  async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.sending.set(true);
    this.error.set(null);
    try {
      const data = this.form.getRawValue();
      await firstValueFrom(
        this.tenant ? this.http.put(`${TENANTS_API}/${this.tenant.id}`, data) : this.http.post(TENANTS_API, data),
      );
      this.ref.close(true);
    } catch (err) {
      this.error.set(Object.values(fieldErrors(err))[0] ?? errorMessage(err));
    } finally {
      this.sending.set(false);
    }
  }
}
