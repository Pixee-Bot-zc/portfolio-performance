package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.LogoManager;

public class FollowUpWidget extends AbstractSecurityListWidget<FollowUpWidget.FollowUpItem>
{
    public static class FollowUpItem extends AbstractSecurityListWidget.Item
    {
        private AttributeType type;
        private LocalDate date;

        public FollowUpItem(Security security, AttributeType type, LocalDate date)
        {
            super(security);
            this.type = type;
            this.date = date;
        }
    }

    public enum DateCheck
    {
        PAST(Messages.OptionDateIsInThePast, date -> !LocalDate.now().isBefore(date)), //
        FUTURE(Messages.OptionDateIsInTheFuture, date -> !date.isBefore(LocalDate.now()));

        private String label;
        private Predicate<LocalDate> predicate;

        private DateCheck(String label, Predicate<LocalDate> predicate)
        {
            this.label = label;
            this.predicate = predicate;
        }

        public boolean include(LocalDate date)
        {
            return predicate.test(date);
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    static class DateDateConfig extends EnumBasedConfig<DateCheck>
    {
        public DateDateConfig(WidgetDelegate<?> delegate)
        {
            super(delegate, Messages.ColumnDate, DateCheck.class, Dashboard.Config.REPORTING_PERIOD,
                            Policy.EXACTLY_ONE);
        }
    }

    public FollowUpWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new AttributesConfig(this, t -> t.getTarget() == Security.class && t.getType() == LocalDate.class));
        addConfig(new DateDateConfig(this));
        addConfig(new ChartHeightConfig(this));
    }

    @Override
    public Supplier<List<FollowUpItem>> getUpdateTask()
    {
        return () -> {

            DateCheck dateType = get(DateDateConfig.class).getValue();
            List<AttributeType> types = get(AttributesConfig.class).getTypes();

            List<FollowUpItem> items = new ArrayList<>();
            for (Security security : getClient().getSecurities())
            {
                for (AttributeType t : types)
                {
                    Object attribute = security.getAttributes().get(t);
                    if (!(attribute instanceof LocalDate))
                        continue;

                    if (dateType.include((LocalDate) attribute))
                    {
                        items.add(new FollowUpItem(security, t, (LocalDate) attribute));
                    }
                }
            }

            return items;
        };
    }

    @Override
    protected Composite createItemControl(Composite parent, FollowUpItem item)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FormLayout());

        Label logo = new Label(composite, SWT.NONE);
        logo.setImage(LogoManager.instance().getDefaultColumnImage(item.getSecurity(), getClient().getSettings()));

        Label name = new Label(composite, SWT.NONE);
        name.setText(item.getSecurity().getName());

        Label date = new Label(composite, SWT.NONE);
        date.setText(item.type.getName() + ": " + Values.Date.format(item.date)); //$NON-NLS-1$

        composite.addMouseListener(mouseUpAdapter);
        name.addMouseListener(mouseUpAdapter);
        date.addMouseListener(mouseUpAdapter);

        FormDataFactory.startingWith(logo).thenRight(name).right(new FormAttachment(100))
                        .thenBelow(date);

        return composite;
    }
}
