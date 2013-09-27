// http://af-design.com/blog/2010/11/10/setting-and-reading-cookies-in-javascript/
var cookie = {
    set: function (name, value, days) {
		var expires,
			date;
        if (days) {
            date = new Date();
            date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
            expires  = "; expires=" + date.toGMTString();
        } else {
			expires = "";
		}
        document.cookie = name + "=" + value + expires + "; path=/";
    },
    get: function (name) {
        var i, c,
			nameEQ = name + "=",
			ca = document.cookie.split(';');

        for (i = 0; i < ca.length; i++) {
            c = ca[i];
            while (c.charAt(0) === ' ') {
				c = c.substring(1, c.length);
			}

            if (c.indexOf(nameEQ) === 0) {
				return c.substring(nameEQ.length, c.length);
			}
        }
        return null;
    },
    del: function (name) {
        this.set(name, "", -1);
    }
};