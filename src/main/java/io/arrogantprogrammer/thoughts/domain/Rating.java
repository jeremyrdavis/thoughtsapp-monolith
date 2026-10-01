package io.arrogantprogrammer.thoughts.domain;

public record Rating(int thumbsUp, int thumbsDown) {

    public Rating {
        if (thumbsUp < 0) {
            throw new IllegalArgumentException("thumbsUp must be >= 0");
        }
        if (thumbsDown < 0) {
            throw new IllegalArgumentException("thumbsDown must be >= 0");
        }
    }

    public static Rating zero() {
        return new Rating(0, 0);
    }

    public Rating incrementThumbsUp() {
        return new Rating(thumbsUp + 1, thumbsDown);
    }

    public Rating incrementThumbsDown() {
        return new Rating(thumbsUp, thumbsDown + 1);
    }

    public double approvalRate() {
        int total = thumbsUp + thumbsDown;
        if (total == 0) {
            return 0.0;
        }
        return (double) thumbsUp / total;
    }
}
