package net.prsv.rengine;

import java.util.Scanner;

public class Main {

    private static String pattern;
    private static StateMachine machine;

    public static void setNewPattern() {
        Scanner s = new Scanner(System.in);
        String input = "";
        while (input.isBlank()) {
            System.out.println("Enter the regular expression: ");
            System.out.print("> ");
            input = s.nextLine();
        }
        Main.pattern = input;
        Main.machine = NFABuilder.build(RegexParser.infixToPostfix(RegexParser.tokenize(pattern)));
    }

    public static void main(String[] args) {

        setNewPattern();

        Scanner s = new Scanner(System.in);
        boolean quit = false;

        while(!quit) {
            System.out.println("Current regex: " + pattern + "\nEnter the input string, type ':regex' to set a new regex pattern, or ':quit' to exit: ");
            System.out.print("> ");
            String input = s.nextLine();
            if(input.trim().equalsIgnoreCase(":quit")) {
                quit = true;
                System.out.println("Bye!");
                continue;
            }
            if(input.trim().equalsIgnoreCase(":summary")) {
                System.out.println(machine.summary());
                continue;
            }
            if(input.trim().equalsIgnoreCase(":regex")) {
                setNewPattern();
                continue;
            }
            boolean accepted = machine.run(input);
            if (accepted) {
                System.out.println("String '" + input + "' accepted.");
            } else {
                System.out.println("String '" + input + "' rejected.");
            }
        }
        s.close();

    }
}
