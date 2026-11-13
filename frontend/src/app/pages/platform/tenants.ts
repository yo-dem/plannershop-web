import { DatePipe } from '@angular/common';
import { HttpClient, httpResource } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { MatProgressBar } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltip } from '@angular/material/tooltip';
import { firstValueFrom } from 'rxjs';

import { errorMessage } from '../../core/errors';
import { Confirm } from '../../core/ui/confirm';
import { Notice } from '../../core/ui/notice';
import { Notifications } from '../../core/ui/notifications';
import { TENANTS_API, TenantDialog, TenantDto } from './tenant-dialog';

/** Management of the platform's tenants (SUPERADMIN only). */
@Component({
  selector: 'app-tenants',
  imports: [DatePipe, MatButton, MatIconButton, MatIcon, MatProgressBar, MatTableModule, MatTooltip, Notice],
  templateUrl: './tenants.html',
  styleUrl: './tenants.scss',
})
export class Tenants {
  private readonly http = inject(HttpClient);
  private readonly dialog = inject(MatDialog);
  private readonly notifications = inject(Notifications);
  private readonly confirm = inject(Confirm);
  protected readonly columns = ['name', 'admins', 'status', 'created', 'actions'];

  protected readonly list = httpResource<TenantDto[]>(() => TENANTS_API, { defaultValue: [] });

  async open(tenant: TenantDto | null): Promise<void> {
    const ref = this.dialog.open<TenantDialog, TenantDto | null, boolean>(TenantDialog, {
      data: tenant,
      width: '30rem',
      maxWidth: '95vw',
    });
    if (await firstValueFrom(ref.afterClosed())) {
      this.notifications.success(
        tenant ? $localize`:@@tenants.updated:Business updated` : $localize`:@@tenants.created:Business created`,
      );
      this.list.reload();
    }
  }

  async delete(t: TenantDto): Promise<void> {
    const name = t.name;
    const ok = await this.confirm.ask({
      title: $localize`:@@tenants.deleteTitle:Delete the business?`,
      message: $localize`:@@tenants.deleteMessage:"${name}:name:" will no longer be accessible and its users will be signed out.`,
      confirm: $localize`:@@common.delete:Delete`,
      danger: true,
    });
    if (!ok) return;
    try {
      await firstValueFrom(this.http.delete(`${TENANTS_API}/${t.id}`));
      this.notifications.success($localize`:@@tenants.deleted:Business deleted`);
      this.list.reload();
    } catch (err) {
      this.notifications.error(errorMessage(err));
    }
  }
}
