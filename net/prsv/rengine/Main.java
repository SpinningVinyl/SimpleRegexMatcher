package net.prsv.rengine;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner s = new Scanner(System.in);
        System.out.println("Enter the regular expression: ");
        System.out.print("> ");
        String pattern = s.nextLine();
        StateMachine machine = NFABuilder.build(RegexParser.infixToPostfix(RegexParser.tokenize(pattern)));


        boolean quit = false;

        while(!quit) {
            System.out.println("Enter the input string, or type ':QUIT' to exit: ");
            System.out.print("> ");
            String input = s.nextLine();
            if(input.trim().equalsIgnoreCase(":quit")) {
                quit = true;
                System.out.println("Bye!");
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
