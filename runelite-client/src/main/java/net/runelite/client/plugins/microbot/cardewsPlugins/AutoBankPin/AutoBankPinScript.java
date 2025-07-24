package net.runelite.client.plugins.microbot.cardewsPlugins.AutoBankPin;

import net.runelite.api.widgets.Widget;
import net.runelite.api.Point;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.cardewsPlugins.CUtil;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.antiban.enums.Activity;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;

import java.util.concurrent.TimeUnit;
//import java.awt.Point;
import java.awt.Rectangle;


public class AutoBankPinScript extends Script {
    Widget bankPinContainerWidget;
    Widget instructionTextWidget;

    int bankPinContainerID = 13959168;
    int instructionTextID = 13959178;
    int[] pinOptionIDs = {
            13959184,
            13959186,
            13959188,
            13959190,
            13959192,
            13959194,
            13959196,
            13959198,
            13959200,
            13959202
    };

    public boolean run(AutoBankPinConfig config) {
        Microbot.enableAutoRunOn = false;
        InitialiseAntiban();
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                if (!super.run()) return;
                long startTime = System.currentTimeMillis();

                bankPinContainerWidget = Rs2Widget.getWidget(bankPinContainerID);
                //System.out.println(("Widget: " + bankPinContainerWidget));
                if (bankPinContainerWidget != null) {
                    //System.out.println(("Bank Pin Interface != null "));
                    //System.out.println(("Bank Pin Interface ID: " + bankPinContainerWidget.getId()));
                    Microbot.bankPinBeingHandled = true;
                    AttemptBankPin(config);
                }

                long endTime = System.currentTimeMillis();
                long totalTime = endTime - startTime;

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, 300, TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    // WidgetID for Bank Pin Container: N 213.0 BANK_PIN_CONTAINER  : 13959168
    // WidgetID - Instruction text: S 213.10                    | ID: 13959178
    // PinOption1: S 213.16 | SpecifiedNumberText: D 213.16[1]  | ID: 13959184
    // PinOption2: S 213.18 | SpecifiedNumberText: D 213.18[1]  | ID: 13959186
    // PinOption3: S 213.20 | SpecifiedNumberText: D 213.20[1]  | ID: 13959188
    // PinOption4: S 213.22 | SpecifiedNumberText: D 213.22[1]  | ID: 13959190
    // PinOption5: S 213.24 | SpecifiedNumberText: D 213.24[1]  | ID: 13959192
    // PinOption6: S 216.26 | SpecifiedNumberText: D 213.26[1]  | ID: 13959194
    // PinOption7: S 213.28 | SpecifiedNumberText: D 213.28[1]  | ID: 13959196
    // PinOption8: S 213.30 | SpecifiedNumberText: D 213.30[1]  | ID: 13959198
    // PinOption9: S 213.32 | SpecifiedNumberText: D 213.32[1]  | ID: 13959200
    // PinOption10: S 213.34 | SpecifiedNumberText: D 213.34[1] | ID: 13959202

    // Fields:
    //      CanvasLocation | Point(x=X,y=Y)
    //      Text | The text drawn. Grab the digit number from this
    //      Hidden | Boolean
    private void AttemptBankPin(AutoBankPinConfig _config)
    {
        int first, second, third, fourth;
        first = _config.digit1();
        second = _config.digit2();
        third = _config.digit3();
        fourth = _config.digit4();
        //System.out.println(("Bank Pin: " + first + second + third + fourth));

        //System.out.println(("The Bank Pin Interface is active"));

        // TODO: Solve the case where if you are already mousing over a pin widget - which hides the number displayed - then move the mouse to uncover the digit
        instructionTextWidget = Rs2Widget.getWidget(instructionTextID);
        if (instructionTextWidget.getText().contains("FIRST")) {
            //System.out.println(("BANK PIN INTERFACE - ENTER FIRST DIGIT"));
            if (!ClickPinWidgetWithDigit(first))
            {
                MoveMouseToRandomPointNearby();
            }
        } else if (instructionTextWidget.getText().contains("SECOND")) {
            //System.out.println(("BANK PIN INTERFACE - ENTER SECOND DIGIT"));
            if (!ClickPinWidgetWithDigit(second))
            {
                MoveMouseToRandomPointNearby();
            }
        } else if (instructionTextWidget.getText().contains("THIRD")) {
            //System.out.println(("BANK PIN INTERFACE - ENTER THIRD DIGIT"));
            if (!ClickPinWidgetWithDigit(third))
            {
                MoveMouseToRandomPointNearby();
            }
        } else if (instructionTextWidget.getText().contains("FOURTH")) {
            //System.out.println(("BANK PIN INTERFACE - ENTER FOURTH DIGIT"));
            if (!ClickPinWidgetWithDigit(fourth))
            {
                MoveMouseToRandomPointNearby();
            }
            else
            {
                Microbot.bankPinBeingHandled = false;
            }
        }
    }

    private boolean ClickPinWidgetWithDigit(int _digit)
    {
        for (int i = 0; i < pinOptionIDs.length; i++)
        {
            Widget optionWidget = Rs2Widget.getWidget(pinOptionIDs[i]);
            //System.out.println(("Option Widget Text: " + optionWidget.getText()));
            if (optionWidget.getChild(1).getText().equals(Integer.toString(_digit)))
            {
                //System.out.println(("This widget is the widget we want to click"));
                //Rs2Widget.clickWidget(optionWidget);
                Rs2Widget.clickWidgetFast(optionWidget);
                return true;
            }
        }
        return false;
    }

    private void MoveMouseToRandomPointNearby()
    {
        // Get a random point somewhere around the mousePoint
        Rectangle randomPointBounds = new Rectangle((int)Microbot.getMouse().getMousePosition().getX() - 50, (int)Microbot.getMouse().getMousePosition().getY() - 50, 100, 100);
        Point newMouseLocation = CUtil.GetRandomPointInRectangle(randomPointBounds);
        // Move mouse to random point.
        Microbot.getMouse().move(newMouseLocation);
    }

    private void InitialiseAntiban()
    {
        Rs2AntibanSettings.antibanEnabled = true;
        Rs2AntibanSettings.usePlayStyle = true;
        Rs2AntibanSettings.simulateFatigue = true;
        Rs2AntibanSettings.simulateAttentionSpan = true;
        Rs2AntibanSettings.behavioralVariability = true;
        Rs2AntibanSettings.nonLinearIntervals = true;
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.moveMouseOffScreen = true;
        Rs2AntibanSettings.contextualVariability = true;
        Rs2AntibanSettings.dynamicIntensity = true;
        Rs2AntibanSettings.moveMouseRandomly = true;
        Rs2AntibanSettings.randomIntervals = true;
        Rs2AntibanSettings.takeMicroBreaks = true;
        Rs2AntibanSettings.microBreakDurationLow = 2;
        Rs2AntibanSettings.microBreakDurationHigh = 15;
        Rs2AntibanSettings.microBreakChance = 0.16;
        Rs2AntibanSettings.actionCooldownChance = 0.1;
        Rs2AntibanSettings.moveMouseRandomlyChance = 0.1;
    }
}
