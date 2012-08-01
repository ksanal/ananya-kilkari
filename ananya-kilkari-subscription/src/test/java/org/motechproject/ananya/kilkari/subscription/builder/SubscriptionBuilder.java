package org.motechproject.ananya.kilkari.subscription.builder;

import org.joda.time.DateTime;
import org.motechproject.ananya.kilkari.subscription.domain.Operator;
import org.motechproject.ananya.kilkari.subscription.domain.Subscription;
import org.motechproject.ananya.kilkari.subscription.domain.SubscriptionPack;
import org.motechproject.ananya.kilkari.subscription.domain.SubscriptionStatus;

public class SubscriptionBuilder {
    private String msisdn;
    private SubscriptionPack pack;
    private Operator operator;
    private SubscriptionStatus status;
    private DateTime creationDate;
    private DateTime startDate;

    public SubscriptionBuilder withDefaults() {
        DateTime now = DateTime.now();
        return withMsisdn("9876543210")
                .withOperator(Operator.AIRTEL)
                .withStatus(SubscriptionStatus.ACTIVE)
                .withPack(SubscriptionPack.FIFTEEN_MONTHS)
                .withCreationDate(now)
                .withStartDate(now);
    }

    public SubscriptionBuilder withStartDate(DateTime now) {
        this.startDate = now;
        return this;
    }

    public SubscriptionBuilder withStatus(SubscriptionStatus status) {
        this.status = status;
        return this;
    }

    public SubscriptionBuilder withOperator(Operator operator) {
        this.operator = operator;
        return this;
    }

    public SubscriptionBuilder withMsisdn(String msisdn) {
        this.msisdn = msisdn;
        return this;
    }

    public SubscriptionBuilder withPack(SubscriptionPack pack) {
        this.pack = pack;
        return this;
    }


    public SubscriptionBuilder withCreationDate(DateTime creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public Subscription build() {
        Subscription subscription = new Subscription(msisdn, pack, creationDate);
        subscription.setStatus(status);
        subscription.setOperator(operator);
        subscription.setStartDate(startDate);
        return subscription;
    }
}
