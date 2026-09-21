package com.timecapsule.ui;

/** Scripted Stage-1 lines for Frieren. */
public class StaticFairyLines implements FairyDialogueProvider {

    @Override
    public String lineFor(String pageId) {
        switch (pageId) {
            case "cover":
                return "Ssh… the grimoire sleeps. Touch the clasp, and I'll show you the pages I guard.";
            case "contents":
                return "Every chapter keeps a different kind of memory. Sealed ones open on their own — no peeking before the date, keeper.";
            case "entries":
                return "Your pages, keeper — newest first. Sealed ones keep their secrets until their day.";
            case "export":
                return "Copies for safekeeping? Wise. The originals never leave my sight.";
            case "capsules":
                return "Red dots are promises, gold ones are memories come true. Patience, keeper.";
            case "calendar":
                return "Every ink-dot is a day you lived twice — once living, once writing.";
            case "habits":
                return "Small marks, kept daily, move mountains. Or so the elves say.";
            case "stats":
                return "The stars only mirror what you wrote. Write kindly.";
            case "denied":
                return "That is not the secret word. The grimoire does not budge. Try again.";
            default:
                return "I watch over these pages. Write something worth keeping.";
        }
    }
}
