package vswe.stevesfactory.components;

import vswe.stevesfactory.lib.Localization;
import vswe.stevesfactory.misc.ConnectionBlockType;

import java.util.List;

public class ComponentMenuEmitters extends ComponentMenuContainer
{
    public ComponentMenuEmitters(FlowComponent parent)
    {
        super(parent, ConnectionBlockType.EMITTER);
    }

    @Override
    public String getName()
    {
        return Localization.EMITTER_MENU.toString();
    }

    @Override
    public void addErrors(List<String> errors)
    {
        if (selectedInventories.isEmpty())
        {
            errors.add(Localization.NO_EMITTER_ERROR.toString());
        }
    }

    @Override
    protected void initRadioButtons()
    {

    }
}
