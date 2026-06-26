package com.team04.domain.dispute.entity;

public enum DisputeStatus {
    RECEIVED {
        @Override
        public boolean canTransitionTo(DisputeStatus next) {
            return next == PENDING;
        }
    },
    PENDING {
        @Override
        public boolean canTransitionTo(DisputeStatus next) {
            return next == RESOLVED || next == REJECTED || next == RECEIVED;
        }
    },
    RESOLVED {
        @Override
        public boolean canTransitionTo(DisputeStatus next) {
            return false;
        }
    },
    REJECTED {
        @Override
        public boolean canTransitionTo(DisputeStatus next) {
            return false;
        }
    };

    public abstract boolean canTransitionTo(DisputeStatus next);
}
