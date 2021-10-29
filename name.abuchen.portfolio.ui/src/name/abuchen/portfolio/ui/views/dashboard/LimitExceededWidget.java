package name.abuchen.portfolio.ui.views.dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.AttributeType;
import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.LimitPrice;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityPrice;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.swt.ColoredLabel;

public class LimitExceededWidget extends AbstractSecurityListWidget<LimitExceededWidget.LimitItem>
{
    public static class LimitItem extends AbstractSecurityListWidget.Item
    {
        private LimitPrice limit;
        private SecurityPrice price;

        public LimitItem(Security security, LimitPrice limit, SecurityPrice price)
        {
            super(security);
            this.limit = limit;
            this.price = price;
        }

    }

    public LimitExceededWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new AttributesConfig(this, t -> t.getTarget() == Security.class && t.getType() == LimitPrice.class));
        addConfig(new ChartHeightConfig(this));
    }

    @Override
    public Supplier<List<LimitItem>> getUpdateTask()
    {
        return () -> {

            List<AttributeType> types = get(AttributesConfig.class).getTypes();

            List<LimitItem> items = new ArrayList<>();

            for (Security security : getClient().getSecurities())
            {
                for (AttributeType t : types)
                {
                    Object attribute = security.getAttributes().get(t);
                    if (!(attribute instanceof LimitPrice))
                        continue;

                    LimitPrice limit = (LimitPrice) attribute;

                    SecurityPrice latest = security.getSecurityPrice(LocalDate.now());
                    if (latest != null && limit.isExceeded(latest))
                    {
                        items.add(new LimitItem(security, limit, latest));
                    }
                }
            }

            Collections.sort(items, (r, l) -> r.getSecurity().getName().compareTo(l.getSecurity().getName()));

            return items;
        };
    }


    @Override
    protected Composite createItemControl(Composite parent, LimitItem item)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FormLayout());

        Label logo = new Label(composite, SWT.NONE);
        logo.setImage(LogoManager.instance().getDefaultColumnImage(item.getSecurity(), getClient().getSettings()));

        Label name = new Label(composite, SWT.NONE);
        name.setText(item.getSecurity().getName());

        ColoredLabel price = new ColoredLabel(composite, SWT.RIGHT);
        price.setBackdropColor(item.limit.getRelationalOperator().isGreater() ? Colors.theme().greenBackground()
                        : Colors.theme().redBackground());
        price.setText(Values.Quote.format(item.getSecurity().getCurrencyCode(), item.price.getValue()));

        Label limit = new Label(composite, SWT.NONE);
        limit.setText(item.limit.toString());

        composite.addMouseListener(mouseUpAdapter);
        name.addMouseListener(mouseUpAdapter);
        limit.addMouseListener(mouseUpAdapter);
        price.addMouseListener(mouseUpAdapter);

        FormDataFactory.startingWith(logo).thenRight(name).right(new FormAttachment(100)).thenBelow(price)
                        .thenRight(limit);

        return composite;
    }
}
