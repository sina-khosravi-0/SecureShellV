package com.securelight.secureshellv;

public class Constants {
    enum Protocol {
        DIRECT_SSH(0), TLS_SSH(1), DUAL_SSH(2);

        public final int index;

        Protocol(int index) {
            this.index = index;
        }
    }

}
