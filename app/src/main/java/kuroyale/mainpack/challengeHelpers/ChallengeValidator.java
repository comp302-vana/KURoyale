package kuroyale.mainpack.challengeHelpers;

import java.util.List;

import kuroyale.cardpack.Card;

//Strategy pattern is in use here: we define an interface for
//different deck validation strategies. Each challenge implements
//its own validation strategy.
public interface ChallengeValidator {
    /**
     * Validates if a deck meets the challenge requirements.
     * "cards": The deck to validate, list of 8 cards
     * returns ValidationResult containing success status with a boolean and error message if invalid
     */

    ValidationResult validateDeck(List<Card> card);

    //Validation result containing boolean status and error message if necessary
    class ValidationResult{
        private final boolean valid;
        private final String errorMessage;

        public ValidationResult(boolean valid, String errorMessage){
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult successful(){
            return new ValidationResult(true,null);
        }

        public static ValidationResult failed(String errorMessage){
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {return valid;}
        public String getErrorMessage() {return errorMessage;}
    }


}
