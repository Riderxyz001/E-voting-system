package com.evoting.evotingsystem.repository.projection;

import com.evoting.evotingsystem.entity.NotificationType;

public interface NotificationTypeCountView {
    NotificationType getType();
    long getTotal();
}
