describe('JavaScript addition operator', function() {
	it('adds two numbers together', function() {
		expect(1 + 2).toEqual(3);
	});
});  

describe('Verify Sarissa availability', function() {
	it('creates a Sarissa instance', function() {
		var sarissa = new Sarissa();
		expect(sarissa).not.toBe(undefined);
	});
}); 

describe('Verify CSWClient availability', function() {
	it('creates a CSWClient instance', function() {
		var cswClient = new CSWClient();
		expect(cswClient).not.toBe(undefined);
	});
});  

