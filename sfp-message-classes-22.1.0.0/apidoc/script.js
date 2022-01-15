/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 */


function setup() {
    var coll = document.getElementsByClassName("collapsible");
    var i;
    for (i = 0; i < coll.length; i++) {
        coll[i].addEventListener("click", function () {
            this.classList.toggle("active");
            var content = this.nextElementSibling;
            if (content.style.display === "block") {
                this.textContent = "(show ▼)"
                content.style.display = "none";
            } else {
                this.textContent = "(hide ▲)"
                content.style.display = "block";
            }
        });
    }
}
